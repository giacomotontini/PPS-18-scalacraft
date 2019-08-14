package io.scalacraft.logic

import java.util.UUID

import akka.actor.{Actor, Props}
import akka.pattern._
import com.typesafe.scalalogging.LazyLogging
import io.scalacraft.logic.messages.Message._
import io.scalacraft.logic.traits.creatures.FarmAnimal
import io.scalacraft.logic.traits.{DefaultTimeout, ImplicitContext}
import io.scalacraft.misc.BiomesToCreatureAndDensity
import io.scalacraft.packets.DataTypes.Position
import io.scalacraft.packets.clientbound.PlayPackets.SpawnMob
import net.querz.nbt.mca.MCAUtil

import scala.concurrent.Future
import scala.concurrent.Future._
import scala.util.{Random, Success}

class CreatureSpawner extends Actor with ImplicitContext with DefaultTimeout with LazyLogging {
  //Mantains the number of player in a chunk, this is identified by his x, z coordinates
  var numberOfPlayersInChunk: Map[(Int, Int), Int] = Map()
  //Mantains the habitation times of every habitated chunk
  var chunkHabitationTime: Map[(Int, Int), Int] = Map()
  //Indicates if creatures have been spawned yet in a given chunk
  var spawnedMobFuturePerChunk: Map[(Int, Int), Future[_]] = Map()
  //used to spawn creatures randomly
  val randomGenerator: Random.type = scala.util.Random

  //Update an indicator of a chunk (i.e numberOfPlayer, habitationTime) and get the actual number of player.
  private[this] def updateChunkIndicators(chunkMapToUpdate: Map[(Int, Int), Int], chunkX: Int, chunkZ: Int,
                                          updateFunction: Int => Int, removeEntryPredicate: Int => Boolean):
  (Map[(Int, Int), Int], Int) = {
    val parameterToBeUpdated = chunkMapToUpdate.getOrElse((chunkX, chunkZ), 0)
    val updatedParameter = updateFunction(parameterToBeUpdated)
    val updatedMap = if (removeEntryPredicate(updatedParameter))
      chunkMapToUpdate.updated((chunkX, chunkZ), updatedParameter)
    else
      chunkMapToUpdate - ((chunkX, chunkZ))

    (updatedMap, updatedParameter)
  }

  private[this] def requestSpawnMobPacketsToCreatures(chunkX: Int, chunkZ: Int, onResult: List[SpawnMob] => Unit): Unit = {
    sequence(context.children.map(mobActor => mobActor ? GetCreatureInChunk(chunkX, chunkZ))).onComplete {
      case Success(spawnMobPackets: List[Option[SpawnMob]]) => onResult(spawnMobPackets.collect {
        case Some(spawnMob: SpawnMob) => spawnMob
      })
    }
  }

  private[this] def getMaximumSquareHeight(positions: List[Position], position: Position): Int = {
    positions.collect {
      case Position(x, y, z) if x == position.x - 1 && z == position.z ||
        z == position.z - 1 && x == position.x ||
        x == position.x - 1 && z == position.z - 1 ||
        x == position.x && z == position.z =>
        println("Original: ", position.x, position.y, position.z, "New :", x, y, z)
        y
    }.max
  }

  override def receive: Receive = {
    case RequestMobsInChunk(x, z/*chunkX, chunkZ*/) =>
      val chunkX = MCAUtil.blockToChunk(x)
      val chunkZ = MCAUtil.blockToChunk(z)
      val senderRef = sender
      val (updatedMap, actualNumberOfPlayer) = updateChunkIndicators(chunkMapToUpdate = numberOfPlayersInChunk,
        chunkX = chunkX, chunkZ = chunkZ, updateFunction = _ + 1, removeEntryPredicate = _ => false)
      numberOfPlayersInChunk = updatedMap
      if (actualNumberOfPlayer == 1) {
        val spawnedMobFuture = (context.parent ? RequestSpawnPoints(chunkX, chunkZ)).andThen {
          case Success(biomeToSpawnPosition: Map[Int, List[(Position, Boolean)]]) =>
            for (biomeAndPosition <- biomeToSpawnPosition;
                 creatureInstance <- BiomesToCreatureAndDensity.creatureInstance
                 if creatureInstance.spawnableBiomes.keySet.contains(biomeAndPosition._1)) yield {
              creatureInstance match {
                case farmAnimal: FarmAnimal =>
                  //val spawnProbability = farmAnimal.spawnableBiomes(biomeAndPosition._1)
                  var positions = biomeAndPosition._2.filter {
                    case (_, isWater) => !isWater
                  }.map(_._1)
                  //if (randomGenerator.nextFloat() < spawnProbability) {
                    //val position = positions(randomGenerator.nextInt(positions.length))
                    val pos = positions.find(p => p.x == x && p.z == z)
                  if(pos.isDefined) {
                    val position = pos.get
                    for (_ <- 0 to 3) {
                      val uuid: UUID = UUID.randomUUID()
                      val entityId = randomGenerator.nextInt()
                      logger.info("spawned mob in: " + position.x + " " + position.y + " " + position.z)
                      context.actorOf(farmAnimal.props(entityId, uuid, position.x, getMaximumSquareHeight(positions, position), position.z,
                        randomGenerator.nextFloat() < farmAnimal.spawnBabyPercentage))
                      //positions = positions.filter(elem => elem != position)
                      // }
                    }
                  }
                  else {
                    logger.info("cannot spawn mob at: " + x + " " + z)
                  }
                case _ => println("Not Implemented Animal")
              }
            }
            requestSpawnMobPacketsToCreatures(chunkX, chunkZ, spawnMobPackets => senderRef ! spawnMobPackets)
            chunkHabitationTime ++= Map((chunkX, chunkZ) -> 0)
        }
        spawnedMobFuturePerChunk ++= Map((chunkX, chunkZ) -> spawnedMobFuture)
      } else {
        if (chunkHabitationTime.contains((chunkX, chunkZ))) {
          requestSpawnMobPacketsToCreatures(chunkX, chunkZ, spawnMobPackets => sender ! spawnMobPackets)
        } else {
          spawnedMobFuturePerChunk((chunkX, chunkZ)).onComplete {
            case Success(_) => requestSpawnMobPacketsToCreatures(chunkX, chunkZ, spawnMobPackets => sender ! spawnMobPackets)
          }
        }
      }

    case PlayerExitedFromChunk(chunkX, chunkZ) =>
      val (updatedMap, actualNumberOfPlayer) = updateChunkIndicators(chunkMapToUpdate = numberOfPlayersInChunk,
        chunkX = chunkX, chunkZ = chunkZ, updateFunction = _ - 1, removeEntryPredicate = _ == 0)
      numberOfPlayersInChunk = updatedMap
      if (actualNumberOfPlayer == 0) {
        chunkHabitationTime = chunkHabitationTime - ((chunkX, chunkZ))
        context.children.foreach(_ ! DespawnCreature(chunkX, chunkZ))
      }
  }
}

object CreatureSpawner {
  def props: Props = Props(new CreatureSpawner)

  def name: String = "creatureSpawner"
}