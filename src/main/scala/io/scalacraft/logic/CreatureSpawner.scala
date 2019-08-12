package io.scalacraft.logic

import java.util.UUID

import akka.actor.{Actor, Props}
import akka.pattern._
import io.scalacraft.logic.messages.Message._
import io.scalacraft.logic.traits.{DefaultTimeout, ImplicitContext}
import io.scalacraft.packets.DataTypes.{Angle, Position}
import io.scalacraft.packets.Entities.Chicken
import io.scalacraft.packets.clientbound.PlayPackets.SpawnMob

import scala.util.Success

class CreatureSpawner extends Actor with ImplicitContext with DefaultTimeout {
  //Mantains the number of player in a chunk, this is identified by his x, z coordinates
  var numberOfPlayersInChunk: Map[(Int, Int), Int] = Map()
  //Mantains the habitation times of every habitated chunk
  var chunkHabitationTime: Map[(Int, Int), Int] = Map()
  var i = 100

  //Update an indicator of a chunk (i.e numberOfPlayer, habitationTime) and get the actual number of player.
  private[this] def updateChunkIndicators(chunkMapToUpdate: Map[(Int, Int), Int], chunkX: Int, chunkZ: Int,
                                             updateFunction: Int => Int, removeEntryPredicate: Int => Boolean):
  (Map[(Int, Int), Int], Int) = {
    val parameterToBeUpdated = chunkMapToUpdate.getOrElse((chunkX, chunkZ),0)
    val updatedParameter = updateFunction(parameterToBeUpdated)
    val updatedMap = if (removeEntryPredicate(updatedParameter))
      chunkMapToUpdate.updated((chunkX, chunkZ), updatedParameter)
    else
      chunkMapToUpdate - ((chunkX, chunkZ))

    (updatedMap, updatedParameter)
  }

  override def receive: Receive = {
    case PlayerEnteredInChunk(chunkX, chunkZ, player) =>
      val (updatedMap, actualNumberOfPlayer) = updateChunkIndicators(chunkMapToUpdate = numberOfPlayersInChunk,
        chunkX = chunkX, chunkZ = chunkZ, updateFunction = _ + 1, removeEntryPredicate = _ => false)
      numberOfPlayersInChunk = updatedMap
      if (actualNumberOfPlayer == 1) {
        chunkHabitationTime = chunkHabitationTime ++ Map((chunkX, chunkZ) -> 0)
        context.parent ? RequestSpawnPoints(chunkX, chunkZ) onComplete{
          case Success(biomeToSpawnPosition: Map[Int, List[Position]]) =>
            biomeToSpawnPosition.values.foreach(values =>values.foreach(position =>{
            player ! SpawnMob(i,UUID.randomUUID(), 7, position.x, position.y+10, position.z, Angle(0), Angle(0),Angle(0), 0, 0, 0, new Chicken());
              i+=1})
            )
        }
      } else {
        context.children.foreach(mobActor => mobActor ? GetCreatureInChunk(chunkX,chunkZ) onComplete{
          ??? //case Success()
        })
      }

    case PlayerExitedFromChunk(chunkX, chunkZ, player) =>
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