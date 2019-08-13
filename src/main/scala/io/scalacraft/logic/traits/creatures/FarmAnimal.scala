package io.scalacraft.logic.traits.creatures

import java.util.UUID

import akka.actor.Props

trait Creature {
  def spawnableBiomes: Map[Int, Double]
  def props(entityId: Int, UUID: UUID, x: Int, y: Int, z: Int, isBaby: Boolean = false): Props
  def name(UUID: UUID): String
}
trait FarmAnimal extends Creature {
  override def spawnableBiomes: Map[Int, Double] = Map(1 -> 0.33, 4 -> 0.25, 5 -> 0.13 )
  val spawnNumber = 4
  val spawnBabyPercentage = 0.1
}


