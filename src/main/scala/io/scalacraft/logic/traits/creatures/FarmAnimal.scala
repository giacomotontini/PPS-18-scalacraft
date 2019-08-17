package io.scalacraft.logic.traits.creatures

import java.util.UUID

import akka.actor.Props
import io.scalacraft.packets.DataTypes
import io.scalacraft.packets.DataTypes.Position

import scala.util.Random

case class SpawnCreatureParameters(biomeToSpawnPosition: Map[Int, Set[(Position, Boolean)]],
                                   spawnPolicy: Position => Set[(Props, String)],
                                   positionFilter: Set[(DataTypes.Position, Boolean)] => Set[Position])
case class SpawnCreatureResult(updatedPosition: Map[Int, Set[(Position, Boolean)]],
                               actorToSpawn: Set[(Props, String)])
trait Creature {
  protected val randomGenerator: Random.type = scala.util.Random
  def spawnableBiomes: Map[Int, Double]
  def props(entityId: Int, UUID: UUID, x: Int, y: Int, z: Int, isBaby: Boolean = false): Props
  def name(UUID: UUID): String
  def spawn(spawnCreatureParameters: SpawnCreatureParameters): SpawnCreatureResult= {
    var unusedPositions: Map[Int, Set[(Position, Boolean)]] = Map()
    var actorToSpawn: Set[(Props, String)] = Set()
    for (biomeAndPosition <- spawnCreatureParameters.biomeToSpawnPosition
         if spawnableBiomes.keySet.contains(biomeAndPosition._1)) yield {
      var positions = spawnCreatureParameters.positionFilter(biomeAndPosition._2)
      val biome = biomeAndPosition._1
      val spawnProbability = spawnableBiomes(biome)
      if (randomGenerator.nextFloat() < spawnProbability && positions.nonEmpty) {
        val position = positions.toVector(randomGenerator.nextInt(positions.size))
        actorToSpawn ++= spawnCreatureParameters.spawnPolicy(position)
        positions -= position
        unusedPositions ++= Map(biome -> biomeAndPosition._2.diff(positions.map(position => (position, false))))
      }
    }
    SpawnCreatureResult(unusedPositions, actorToSpawn)
  }
}
trait FarmAnimal extends Creature {
  override def spawnableBiomesToDensity: Map[Int, Float] = Map(1 -> 0.33f, 4 -> 0.25f, 5 -> 0.13f )
  val spawnNumber = 4
  val spawnBabyPercentage = 0.1
  def spawn(biomeToSpawnPosition: Map[Int, Set[(Position, Boolean)]]): SpawnCreatureResult = {
    val spawnGroup: Position => Set[(Props, String)] = {
      position =>
      (for (_ <- 0 until spawnNumber) yield{
        val uuid: UUID = UUID.randomUUID()
        val entityId = randomGenerator.nextInt()
        val actorProps = props(entityId, uuid, position.x, position.y, position.z, randomGenerator.nextFloat() < spawnBabyPercentage)
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


