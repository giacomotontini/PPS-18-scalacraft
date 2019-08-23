package io.scalacraft.logic.traits.creatures

import java.util.UUID

import akka.actor.{ActorRef, Props}
import akka.pattern._
import io.scalacraft.logic.inventories.traits.DefaultTimeout
import io.scalacraft.logic.messages.Message.RequestEntityId
import io.scalacraft.packets.DataTypes
import io.scalacraft.packets.DataTypes.Position

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random

case class SpawnCreatureParameters(biomeToSpawnPosition: Map[Int, Set[(Position, Boolean)]],
                                   spawnPolicy: Position => Set[(Props, String)],
                                   positionFilter: Set[(DataTypes.Position, Boolean)] => Set[Position])
case class SpawnCreatureResult(updatedPosition: Map[Int, Set[(Position, Boolean)]],
                               actorToSpawn: Set[(Props, String)])
trait Creature {
  protected val randomGenerator: Random.type = scala.util.Random
  def spawnableBiomes: Map[Int, Double]
  def props(entityId: Int, UUID: UUID, x: Int, y: Int, z: Int, isBaby: Boolean = false, world: ActorRef): Props
  def name(UUID: UUID): String
  protected def spawn(spawnCreatureParameters: SpawnCreatureParameters): SpawnCreatureResult= {
    var unusedPositions: Map[Int, Set[(Position, Boolean)]] = spawnCreatureParameters.biomeToSpawnPosition
    var actorToSpawn: Set[(Props, String)] = Set()
    for (biomeAndPosition <- spawnCreatureParameters.biomeToSpawnPosition
         if spawnableBiomes.keySet.contains(biomeAndPosition._1)) {
      var positions = spawnCreatureParameters.positionFilter(biomeAndPosition._2)
      val biome = biomeAndPosition._1
      val spawnProbability = spawnableBiomes(biome) /256
      if (randomGenerator.nextFloat() < spawnProbability && positions.nonEmpty) {
        val position = positions.toVector(randomGenerator.nextInt(positions.size))
        actorToSpawn ++= spawnCreatureParameters.spawnPolicy(position)
        positions -= position
        unusedPositions = Map(biome -> positions.map(position => (position, false)))
      }
    }
    SpawnCreatureResult(unusedPositions, actorToSpawn)
  }
}
trait FarmAnimal extends Creature with DefaultTimeout{
  override def spawnableBiomes: Map[Int, Double] = Map(1 -> 10, 4 -> 8, 5 -> 5 )
  val spawnNumber = 4
  val spawnBabyPercentage = 0.1
  def spawn(biomeToSpawnPosition: Map[Int, Set[(Position, Boolean)]], world: ActorRef): SpawnCreatureResult = {
    val spawnGroup: Position => Set[(Props, String)] = {
      position =>
        (for (_ <- 0 until spawnNumber) yield{
          val uuid: UUID = UUID.randomUUID()
          val entityId = Await.result((world ? RequestEntityId).mapTo[Int], 2000 millisecond)
          val actorProps = props(entityId, uuid, position.x, position.y, position.z, randomGenerator.nextFloat() < spawnBabyPercentage, world)
          val actorName = name(uuid)
          (actorProps,actorName)
        }).toSet
    }
    val positionFilter: Set[(DataTypes.Position, Boolean)] => Set[Position] = {
      biomeAndPosition =>
        biomeAndPosition.collect {
          case (position, isWater) if !isWater => position
        }
    }
    spawn(SpawnCreatureParameters(biomeToSpawnPosition, spawnGroup, positionFilter))
  }
}


