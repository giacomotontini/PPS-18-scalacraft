package io.scalacraft.logic

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
import akka.pattern._
import com.typesafe.scalalogging.LazyLogging
import io.scalacraft.logic.creatures.misc.CreatureInstances
import io.scalacraft.logic.inventories.traits.{DefaultTimeout, ImplicitContext}
import io.scalacraft.logic.messages.Message
import io.scalacraft.logic.messages.Message.SkyUpdateState.SkyUpdateState
import io.scalacraft.logic.messages.Message._
import io.scalacraft.logic.traits.creatures.FarmAnimal
import io.scalacraft.packets.DataTypes.Position
import io.scalacraft.packets.clientbound.PlayPackets.SpawnMob

import scala.concurrent.Future
import scala.concurrent.Future._
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Random, Success}

class CreatureSpawner extends Actor with ImplicitContext with DefaultTimeout with ActorLogging {
  //Mantains the number of player in a chunk, this is identified by his x, z coordinates
  var numberOfPlayersInChunk: Map[(Int, Int), Int] = Map()
  //Mantains the habitation times of every habitated chunk
  var chunkHabitationTime: Map[(Int, Int), Int] = Map()
  //Indicates if creatures have been spawned yet in a given chunk
  var spawnedMobFuturePerChunk: Map[(Int, Int), Future[_]] = Map()
  //used to spawn creatures randomly
  var spawnedActor: Set[ActorRef] = Set()
  val randomGenerator: Random.type = scala.util.Random
  val timeout: FiniteDuration = 20 seconds
  var isDay: Boolean = _

  //Update an indicator of a chunk (i.e numberOfPlayer, habitationTime) and get the actual number of player.
  private[this] def updateChunkIndicators(chunkMapToUpdate: Map[(Int, Int), Int], chunkX: Int, chunkZ: Int,
                                          updateFunction: Int => Int, removeEntryPredicate: Int => Boolean):
  (Map[(Int, Int), Int], Int) = {
    val parameterToBeUpdated = chunkMapToUpdate.getOrElse((chunkX, chunkZ), 0)
    val updatedParameter = updateFunction(parameterToBeUpdated)
    val updatedMap = if (removeEntryPredicate(updatedParameter))
      chunkMapToUpdate - ((chunkX, chunkZ))
    else
      chunkMapToUpdate.updated((chunkX, chunkZ), updatedParameter)
    (updatedMap, updatedParameter)
  }

  private[this] def askSomethingToCreatures[T](request: Message, onResult: List[T] => Unit, removeActor: Boolean = false): Unit = {
    sequence(spawnedActor.map(mobActor => mobActor.ask(request)).toList).onComplete {
      case Success(mobResponse) =>
        onResult(mobResponse.collect {
          case AskResponse(senderRef, response) if response.isInstanceOf[Some[_]] =>
            if (removeActor) {
              senderRef ! PoisonPill
              spawnedActor -= senderRef
            }
            response.asInstanceOf[Some[T]].get
        })
      case Failure(ex) => log.error(ex, "Request failed at askSomethingToCreatures")
    }
  }

  override def receive: Receive = {
    case SpawnCreaturesInChunk(chunkX, chunkZ) =>
      val senderRef = sender
      val (updatedMap, _) = updateChunkIndicators(chunkMapToUpdate = numberOfPlayersInChunk,
        chunkX = chunkX, chunkZ = chunkZ, updateFunction = _ + 1, removeEntryPredicate = _ => false)
      numberOfPlayersInChunk = updatedMap
      if (!spawnedMobFuturePerChunk.contains(chunkX, chunkZ)) {
        val spawnedMobFuture = context.parent.ask(RequestSpawnPoints(chunkX, chunkZ))(timeout).mapTo[Map[Int, Set[(Position, Boolean)]]].andThen {
          case Success(biomeToSpawnPosition) =>
            var spawnablePosition = biomeToSpawnPosition
            CreatureInstances.creatureInstances.foreach {
              case farmAnimal: FarmAnimal if isDay =>
                val spawnResult = farmAnimal.spawn(spawnablePosition, context.parent)
                spawnablePosition = spawnResult.updatedPosition
                spawnedActor ++= spawnResult.actorToSpawn.map {
                  case (props: Props, name: String) => context.actorOf(props, name)
                }
            }
            askSomethingToCreatures[SpawnMob](RequestCreatureInChunk(chunkX, chunkZ), spawnMobPackets => senderRef ! spawnMobPackets)
            chunkHabitationTime ++= Map((chunkX, chunkZ) -> 0)
        }
        spawnedMobFuturePerChunk ++= Map((chunkX, chunkZ) -> spawnedMobFuture)
      } else {
        spawnedMobFuturePerChunk((chunkX, chunkZ)).onComplete {
          case Success(_) => askSomethingToCreatures[SpawnMob](RequestCreatureInChunk(chunkX, chunkZ), spawnMobPackets => senderRef ! spawnMobPackets)
          case _ => //do nothing
        }
      }

    case PlayerUnloadedChunk(chunkX, chunkZ) =>
      val senderRef = sender
      val (updatedMap, actualNumberOfPlayer) = updateChunkIndicators(chunkMapToUpdate = numberOfPlayersInChunk,
        chunkX = chunkX, chunkZ = chunkZ, updateFunction = _ - 1, removeEntryPredicate = _ == 0)
      numberOfPlayersInChunk = updatedMap
      if (spawnedMobFuturePerChunk.contains(chunkX, chunkZ) && actualNumberOfPlayer == 0) {
        chunkHabitationTime = chunkHabitationTime - ((chunkX, chunkZ))
        askSomethingToCreatures[DespawnCreature](DespawnCreature(chunkX, chunkZ), despawnCreature => senderRef ! despawnCreature)
        spawnedMobFuturePerChunk = spawnedMobFuturePerChunk - ((chunkX, chunkZ))
      }
    case SkyStateUpdate(state: SkyUpdateState) => state match {
      case SkyUpdateState.Sunrise => isDay = true
      case SkyUpdateState.Noon => isDay = true
      case SkyUpdateState.Sunset => isDay = false
      case SkyUpdateState.MidNight => isDay = false
    }
  }
}

object CreatureSpawner {
  def props: Props = Props(new CreatureSpawner)

  def name: String = "creatureSpawner"
}