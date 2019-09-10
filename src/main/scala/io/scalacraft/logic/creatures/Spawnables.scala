package io.scalacraft.logic.creatures

import java.util.UUID

import akka.actor.{ActorRef, Props}
import akka.pattern._
import io.scalacraft.core.packets.DataTypes.Position
import io.scalacraft.logic.commons.DefaultTimeout
import io.scalacraft.logic.commons.Message.RequestEntityId

import scala.concurrent.Await
import scala.util.Random

object Spawnables {

  type PropsWithName = (Props, String)

  case class PositionWithProperties(position: Position, isWater: Boolean)

  /**
   * Parameters of spawn method.
   * @param biomeToSpawnPosition map that connect each biome in a chunk to all spawn position.
   * @param spawnPolicy the spawn rule
   * @param isWaterCreature true if the creaure is a water creauture, false otherwise.
   */

  case class SpawnCreatureParameters(biomeToSpawnPosition: Map[Int, Set[PositionWithProperties]],
                                     spawnPolicy: Position => Set[PropsWithName],
                                     isWaterCreature: Boolean)

  /**
   * Results of spawn method.
   * @param updatedPosition map that connect each biome in a chunk to all spawn position (considering those already used).
   * @param actorToSpawn the set of actor to be spawned.
   */

  case class SpawnCreatureResult(updatedPosition: Map[Int, Set[PositionWithProperties]],
                                 actorToSpawn: Set[PropsWithName])

  /**
   * Represents generic spawnable creatures.
   */

  trait SpawnableCreature {
    val randomGenerator: Random = scala.util.Random

    /**
     * Connect each creature spawnable biome to his spawn probability
     */
    def spawnableBiomes: Map[Int, Double]

    def props(entityId: Int, UUID: UUID, x: Int, y: Int, z: Int, isBaby: Boolean = false, world: ActorRef): Props

    def name(UUID: UUID): String

    /**
     * Spawn the creature in a random position using probability
     * @param spawnCreatureParameters the method parameters (i.e biomeToSpawnPosition, spawnPolicy, isWaterCreature)
     * @return the unused position, the list of actor to be spawned
     */
    def spawn(spawnCreatureParameters: SpawnCreatureParameters): SpawnCreatureResult = {

      def positionFilter(positionsWithProperties: Set[PositionWithProperties]): Set[Position] = positionsWithProperties.collect {
        case PositionWithProperties(position, isWater) if isWater == spawnCreatureParameters.isWaterCreature => position
      }

      var unusedPositions = spawnCreatureParameters.biomeToSpawnPosition
      var actorToSpawn = Set[PropsWithName]()
      for (biomeAndPositions <- spawnCreatureParameters.biomeToSpawnPosition
           if spawnableBiomes.keySet.contains(biomeAndPositions._1)) {
        val biome = biomeAndPositions._1
        val spawnProbability = spawnableBiomes(biome)
        for (position <- positionFilter(biomeAndPositions._2)) {
          if (randomGenerator.nextDouble() < spawnProbability) {
            actorToSpawn ++= spawnCreatureParameters.spawnPolicy(position)
            unusedPositions = unusedPositions.updated(biome, unusedPositions(biome) - PositionWithProperties(position,
              isWater = spawnCreatureParameters.isWaterCreature))
          }
        }
      }
      SpawnCreatureResult(unusedPositions, actorToSpawn)
    }
  }

  /**
   * Represent spawnable farm animal: chickens, sheeps, pigs and cows.
   */
  trait SpawnableFarmAnimal extends SpawnableCreature with DefaultTimeout {
    override def spawnableBiomes: Map[Int, Double] = Map(1 -> 0.00008, 4 -> 0.00004, 5 -> 0.00003)

    val spawnNumber = 4
    val spawnBabyPercentage = 0.1

    def spawnFarmAnimal(biomeToSpawnPosition: Map[Int, Set[PositionWithProperties]],
                        world: ActorRef): SpawnCreatureResult = {
      val spawnGroup: Position => Set[PropsWithName] = position =>
        (for (_ <- 0 until spawnNumber) yield {
          val uuid: UUID = UUID.randomUUID()
          val entityId = Await.result((world ? RequestEntityId).mapTo[Int], defaultTimeout.duration)
          val actorProps = props(entityId, uuid, position.x, position.y, position.z,
            randomGenerator.nextFloat() < spawnBabyPercentage, world)
          val actorName = name(uuid)
          (actorProps, actorName)
        }).toSet

      spawn(SpawnCreatureParameters(biomeToSpawnPosition, spawnGroup, isWaterCreature = false))
    }
  }

}
