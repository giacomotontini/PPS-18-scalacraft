package io.scalacraft.logic.creatures

import java.util.UUID

import akka.actor.{ActorRef, Props}
import akka.pattern._
import io.scalacraft.logic.commons.DefaultTimeout
import io.scalacraft.logic.commons.Message.RequestEntityId
import io.scalacraft.packets.DataTypes.Position

import scala.concurrent.Await
import scala.util.Random

object Spawnables {

  type PropsWithName = (Props, String)

  case class PositionWithProperties(position: Position, isWater: Boolean)

  case class SpawnCreatureParameters(biomeToSpawnPosition: Map[Int, Set[PositionWithProperties]],
                                     spawnPolicy: Position => Set[PropsWithName],
                                     positionFilter: Set[PositionWithProperties] => Set[Position])

  case class SpawnCreatureResult(updatedPosition: Map[Int, Set[PositionWithProperties]],
                                 actorToSpawn: Set[PropsWithName])

  trait SpawnableCreature {
    protected val randomGenerator: Random.type = scala.util.Random

    def spawnableBiomes: Map[Int, Double]

    def props(entityId: Int, UUID: UUID, x: Int, y: Int, z: Int, isBaby: Boolean = false, world: ActorRef): Props

    def name(UUID: UUID): String

    protected def spawn(spawnCreatureParameters: SpawnCreatureParameters): SpawnCreatureResult = {
      var unusedPositions = spawnCreatureParameters.biomeToSpawnPosition
      assert(unusedPositions.isInstanceOf[Map[Int, Set[Spawnables.PositionWithProperties]]])
      var actorToSpawn = Set[PropsWithName]()
      for (biomeAndPosition <- spawnCreatureParameters.biomeToSpawnPosition
           if spawnableBiomes.keySet.contains(biomeAndPosition._1)) {
        assert(biomeAndPosition._2.isInstanceOf[Set[PositionWithProperties]])
        var positions = spawnCreatureParameters.positionFilter(biomeAndPosition._2)
        val biome = biomeAndPosition._1
        val spawnProbability = spawnableBiomes(biome) / 256
        if (randomGenerator.nextFloat() < spawnProbability && positions.nonEmpty) {
          val position = positions.toVector(randomGenerator.nextInt(positions.size))
          actorToSpawn ++= spawnCreatureParameters.spawnPolicy(position)
          positions -= position
          unusedPositions = Map(biome -> positions.map(position => PositionWithProperties(position, isWater = false))) // TODO: sistemare
        }
      }
      SpawnCreatureResult(unusedPositions, actorToSpawn)
    }
  }

  trait SpawnableFarmAnimal extends SpawnableCreature with DefaultTimeout {
    override def spawnableBiomes: Map[Int, Double] = Map(1 -> 10, 4 -> 8, 5 -> 5)

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

      val positionFilter: Set[PositionWithProperties] => Set[Position] = _.collect {
        case PositionWithProperties(position, isWater) if !isWater => position
      }

      spawn(SpawnCreatureParameters(biomeToSpawnPosition, spawnGroup, positionFilter))
    }
  }

}
