package io.scalacraft.logic

import java.util.UUID

import akka.actor.{Actor, ActorRef, PoisonPill, Props}
import akka.pattern._
import com.typesafe.scalalogging.LazyLogging
import io.scalacraft.logic.messages.Message
import io.scalacraft.logic.messages.Message._
import io.scalacraft.logic.traits.creatures.FarmAnimal
import io.scalacraft.logic.traits.{DefaultTimeout, ImplicitContext}
import io.scalacraft.misc.BiomesToCreatureAndDensity
import io.scalacraft.packets.DataTypes.Position
import io.scalacraft.packets.clientbound.PlayPackets.SpawnMob

import scala.concurrent.Future
import scala.concurrent.Future._
import scala.concurrent.duration._
import scala.util.{Failure, Random, Success}

class CreatureSpawner extends Actor with ImplicitContext with DefaultTimeout with LazyLogging {
  //Mantains the number of player in a chunk, this is identified by his x, z coordinates
  var numberOfPlayersInChunk: Map[(Int, Int), Int] = Map()
  //Mantains the habitation times of every habitated chunk
  var chunkHabitationTime: Map[(Int, Int), Int] = Map()
  //Indicates if creatures have been spawned yet in a given chunk
  var spawnedMobFuturePerChunk: Map[(Int, Int), Future[_]] = Map()
  //used to spawn creatures randomly
  var spawnedActor: Set[ActorRef] = Set()
  val randomGenerator: Random.type = scala.util.Random
  val timeout: FiniteDuration = 10 seconds

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

  private[this] def tmp[T](request: Message, onResult: List[T] => Unit, removeActor: Boolean = false): Unit = {
    sequence(spawnedActor.map(mobActor => mobActor.ask(request)(timeout)).toList).onComplete {
      case Success(mobPackets: List[Option[T]]) =>
        onResult(mobPackets.collect {
          case mobPacket: Some[T] =>
            mobPacket.get
        })
      case Failure(ex) => println("Request failed " + ex.getMessage)
    }
  }

  private[this] def askSomethingToCreatures[T](request: Message, onResult: List[T] => Unit, removeActor: Boolean = false): Unit = {
    sequence(spawnedActor.map(mobActor => mobActor.ask(request)(timeout)).toList).onComplete {
      case Success(mobResponse: List[AskResponse]) =>
        onResult(mobResponse.collect {
          case AskResponse(senderRef, response) if response.isInstanceOf[Some[_]] =>
            if (removeActor) {
              senderRef ! PoisonPill
              spawnedActor -= senderRef
            }
            response.asInstanceOf[Some[T]].get
        })
      case Failure(ex) => println("Request failed " + ex.getMessage)
    }
  }

  private[this] def getMaximumSquareHeight(positions: Set[Position], position: Position): Int = {
    positions.collect {
      case Position(x, y, z) if x == position.x - 1 && z == position.z ||
        z == position.z - 1 && x == position.x ||
        x == position.x - 1 && z == position.z - 1 ||
        x == position.x && z == position.z =>
        y
    }.max
  }

  override def receive: Receive = {
    case RequestMobsInChunk(chunkX, chunkZ) =>
      val senderRef = sender
      val (updatedMap, actualNumberOfPlayer) = updateChunkIndicators(chunkMapToUpdate = numberOfPlayersInChunk,
        chunkX = chunkX, chunkZ = chunkZ, updateFunction = _ + 1, removeEntryPredicate = _ => false)
      numberOfPlayersInChunk = updatedMap
      if (!spawnedMobFuturePerChunk.contains(chunkX, chunkZ)) {
        val spawnedMobFuture = (context.parent ? RequestSpawnPoints(chunkX, chunkZ)).andThen {
          case Success(biomeToSpawnPosition: Map[Int, Set[(Position, Boolean)]]) =>
            for (biomeAndPosition <- biomeToSpawnPosition;
                 creatureInstance <- BiomesToCreatureAndDensity.creatureInstance
                 if creatureInstance.spawnableBiomes.keySet.contains(biomeAndPosition._1)) yield {
              creatureInstance match {
                case farmAnimal: FarmAnimal =>
                  val spawnProbability = farmAnimal.spawnableBiomes(biomeAndPosition._1)
                  var positions = biomeAndPosition._2.collect {
                    case (position, isWater) if !isWater => position
                  }
                  if (randomGenerator.nextFloat() < spawnProbability && positions.nonEmpty) {
                    val position = positions.toVector(randomGenerator.nextInt(positions.size))
                    for (_ <- 0 to 3) {
                      val uuid: UUID = UUID.randomUUID()
                      val entityId = randomGenerator.nextInt()
                      val actorRef = context.actorOf(farmAnimal.props(entityId, uuid, position.x, getMaximumSquareHeight(positions, position), position.z,
                        randomGenerator.nextFloat() < farmAnimal.spawnBabyPercentage))
                      spawnedActor += actorRef
                    }
                    positions -= position
                  }
                case _ => println("Not Implemented Animal")
              }
            }
            askSomethingToCreatures[SpawnMob](GetCreatureInChunk(chunkX, chunkZ), spawnMobPackets => senderRef ! spawnMobPackets)
            chunkHabitationTime ++= Map((chunkX, chunkZ) -> 0)
        }
        spawnedMobFuturePerChunk ++= Map((chunkX, chunkZ) -> spawnedMobFuture)
      } else {
        spawnedMobFuturePerChunk((chunkX, chunkZ)).onComplete {
          case Success(_) => askSomethingToCreatures[SpawnMob](GetCreatureInChunk(chunkX, chunkZ), spawnMobPackets => senderRef ! spawnMobPackets)
        }
      }

    case PlayerUnloadedChunk(chunkX, chunkZ) =>
      val senderRef = sender
      val (updatedMap, actualNumberOfPlayer) = updateChunkIndicators(chunkMapToUpdate = numberOfPlayersInChunk,
        chunkX = chunkX, chunkZ = chunkZ, updateFunction = _ - 1, removeEntryPredicate = _ == 0)
      numberOfPlayersInChunk = updatedMap
      if (spawnedMobFuturePerChunk.contains(chunkX, chunkZ) && spawnedMobFuturePerChunk(chunkX, chunkZ).isCompleted) {
        chunkHabitationTime = chunkHabitationTime - ((chunkX, chunkZ))
        askSomethingToCreatures[DespawnCreature](DespawnCreature(chunkX, chunkZ), despawnCreature => senderRef ! despawnCreature)
        spawnedMobFuturePerChunk = spawnedMobFuturePerChunk - ((chunkX, chunkZ))
      }
  }
}

object CreatureSpawner {
  def props: Props = Props(new CreatureSpawner)

  def name: String = "creatureSpawner"
}