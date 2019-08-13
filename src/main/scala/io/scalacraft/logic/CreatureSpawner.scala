package io.scalacraft.logic

import java.util.UUID

import akka.actor.{Actor, Props}
import akka.pattern._
import io.scalacraft.logic.messages.Message._
import io.scalacraft.logic.traits.{DefaultTimeout, ImplicitContext}
import io.scalacraft.packets.DataTypes.{Angle, Position}
import io.scalacraft.packets.Entities.Chicken
import io.scalacraft.packets.clientbound.PlayPackets.SpawnMob

import scala.concurrent.Future
import scala.concurrent.Future._
import scala.util.Success

class CreatureSpawner extends Actor with ImplicitContext with DefaultTimeout {
  //Mantains the number of player in a chunk, this is identified by his x, z coordinates
  var numberOfPlayersInChunk: Map[(Int, Int), Int] = Map()
  //Mantains the habitation times of every habitated chunk
  var chunkHabitationTime: Map[(Int, Int), Int] = Map()
  //Indicates if creatures have been spawned yet in a given chunk
  var spawnedMobFuturePerChunk: Map[(Int, Int),Future[_]] = _

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
  private[this] def requestSpawnMobPacketsToCreatures(chunkX: Int, chunkZ: Int, onResult: List[SpawnMob] => Unit): Unit  = {
    sequence(context.children.map(mobActor => mobActor ? GetCreatureInChunk(chunkX, chunkZ))).onComplete {
      case Success(spawnMobPackets: List[Option[SpawnMob]]) => onResult(spawnMobPackets.collect {
        case Some(spawnMob: SpawnMob) => spawnMob
      })
    }
  }

  override def receive: Receive = {
    case RequestMobsInChunk(chunkX, chunkZ) =>
      val (updatedMap, actualNumberOfPlayer) = updateChunkIndicators(chunkMapToUpdate = numberOfPlayersInChunk,
        chunkX = chunkX, chunkZ = chunkZ, updateFunction = _ + 1, removeEntryPredicate = _ => false)
      numberOfPlayersInChunk = updatedMap
      if (actualNumberOfPlayer == 1) {
        val spawnedMobFuture = (context.parent ? RequestSpawnPoints(chunkX, chunkZ)).andThen {
          case Success(biomeToSpawnPosition: Map[Int, List[Position]]) =>
            //createActors
            println(biomeToSpawnPosition)
            requestSpawnMobPacketsToCreatures(chunkX,chunkZ, spawnMobPackets => sender ! spawnMobPackets)
            chunkHabitationTime ++= Map((chunkX, chunkZ) -> 0)
        }
        spawnedMobFuturePerChunk ++= Map((chunkX, chunkZ) -> spawnedMobFuture)
      } else {
        if (chunkHabitationTime.contains((chunkX, chunkZ))) {
          requestSpawnMobPacketsToCreatures(chunkX,chunkZ, spawnMobPackets => sender ! spawnMobPackets)
        } else {
          spawnedMobFuturePerChunk((chunkX, chunkZ)).onComplete{
            case Success(_) => requestSpawnMobPacketsToCreatures(chunkX,chunkZ, spawnMobPackets => sender ! spawnMobPackets)
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