package io.scalacraft.logic

import akka.actor.{ActorRef, PoisonPill, Props}
import akka.pattern._
import io.scalacraft.logic.commons.Message
import io.scalacraft.logic.commons.Message.SkyUpdateState.SkyUpdateState
import io.scalacraft.logic.commons.Message._
import io.scalacraft.logic.commons.Traits.EnrichedActor
import io.scalacraft.logic.creatures.CreatureInstances
import io.scalacraft.logic.creatures.Spawnables.{PositionWithProperties, SpawnableFarmAnimal}
import io.scalacraft.packets.clientbound.PlayPackets.SpawnMob

import scala.concurrent.Future
import scala.concurrent.Future._
import scala.concurrent.duration._
import scala.util.{Failure, Random, Success}

class CreatureSpawner extends EnrichedActor {

  type ChunkLocation = (Int, Int)

  //Mantains the number of player in a chunk, this is identified by his x, z coordinates
  var numberOfPlayersInChunk: Map[ChunkLocation, Int] = Map()
  //Indicates if creatures have been spawned yet in a given chunk
  var spawnedMobFuturePerChunk: Map[ChunkLocation, Future[_]] = Map()
  //used to spawn creatures randomly
  var spawnedActor: Set[ActorRef] = Set()
  val randomGenerator: Random = scala.util.Random
  val timeout: FiniteDuration = 20 seconds
  var isDay: Boolean = _

  //Update an indicator of a chunk (i.e numberOfPlayer, habitationTime) and get the actual number of player.
  private[this] def updateChunkIndicators(chunkMapToUpdate: Map[ChunkLocation, Int], chunkX: Int, chunkZ: Int,
                                          updateFunction: Int => Int, removeEntryPredicate: Int => Boolean):
  (Map[ChunkLocation, Int], Int) = {
    val parameterToBeUpdated = chunkMapToUpdate.getOrElse((chunkX, chunkZ), 0)
    val updatedParameter = updateFunction(parameterToBeUpdated)
    val updatedMap = if (removeEntryPredicate(updatedParameter)) chunkMapToUpdate - ((chunkX, chunkZ))
    else chunkMapToUpdate.updated((chunkX, chunkZ), updatedParameter)
    (updatedMap, updatedParameter)
  } // TODO: remove this method

  private[this] def askSomethingToCreatures[T](request: Message, onResult: List[T] => Unit,
                                               removeActor: Boolean = false): Unit = {
    sequence(spawnedActor.map(actor => actor ? request).toList) onComplete {
      case Success(mobResponse) =>
        onResult(mobResponse collect {
          case AskResponse(senderRef, Some(response)) =>
            if (removeActor) {
              senderRef ! PoisonPill
              spawnedActor -= senderRef
            }
            response.asInstanceOf[T]
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
        val spawnedMobFuture = context.parent.ask(RequestSpawnPoints(chunkX, chunkZ))(timeout)
          .mapTo[Map[Int, Set[PositionWithProperties]]].andThen {
          case Success(biomeToSpawnPosition) =>
            var spawnablePosition = biomeToSpawnPosition
            CreatureInstances.creatureInstances foreach {
              case farmAnimal: SpawnableFarmAnimal if isDay =>
                val spawnResult = farmAnimal.spawnFarmAnimal(spawnablePosition, context.parent)
                spawnablePosition = spawnResult.updatedPosition
                spawnedActor ++= spawnResult.actorToSpawn.map {
                  case (props: Props, name: String) => context.actorOf(props, name)
                }
              case _ => // none
            }
            askSomethingToCreatures[SpawnMob](RequestCreatureInChunk(chunkX, chunkZ), senderRef ! _)
        }
        spawnedMobFuturePerChunk ++= Map((chunkX, chunkZ) -> spawnedMobFuture)
      } else {
        spawnedMobFuturePerChunk((chunkX, chunkZ)).onComplete {
          case Success(_) => askSomethingToCreatures[SpawnMob](RequestCreatureInChunk(chunkX, chunkZ), senderRef ! _)
          case _ => //do nothing
        }
      }

    case PlayerUnloadedChunk(chunkX, chunkZ) =>
      val senderRef = sender
      val (updatedMap, actualNumberOfPlayer) = updateChunkIndicators(chunkMapToUpdate = numberOfPlayersInChunk,
        chunkX = chunkX, chunkZ = chunkZ, updateFunction = _ - 1, removeEntryPredicate = _ == 0)
      numberOfPlayersInChunk = updatedMap
      if (spawnedMobFuturePerChunk.contains(chunkX, chunkZ) && actualNumberOfPlayer == 0) {
        askSomethingToCreatures[DespawnCreature](DespawnCreature(chunkX, chunkZ), senderRef ! _)
        spawnedMobFuturePerChunk = spawnedMobFuturePerChunk - ((chunkX, chunkZ))
      }

    case SkyStateUpdate(state: SkyUpdateState) => state match {
      case SkyUpdateState.Sunrise | SkyUpdateState.Noon => isDay = true
      case SkyUpdateState.Sunset | SkyUpdateState.MidNight => isDay = false
    }

    case useEntityWithItem: UseEntityWithItem => spawnedActor foreach (_.forward(useEntityWithItem))

    case entityDead: EntityDead =>
      context.parent forward entityDead
      spawnedActor -= sender
      sender ! PoisonPill
  }
}

object CreatureSpawner {

  def props: Props = Props(new CreatureSpawner)

  def name: String = "creatureSpawner"

}
