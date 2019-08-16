package io.scalacraft.logic.traits.creatures

import java.util.UUID

import akka.actor.Props

trait Creature {
  def spawnableBiomesToDensity: Map[Int, Float]
  def props(entityId: Int, UUID: UUID, x: Int, y: Int, z: Int, isBaby: Boolean = false): Props
  def name(UUID: UUID): String
}
trait FarmAnimal extends Creature {
  override def spawnableBiomesToDensity: Map[Int, Float] = Map(1 -> 0.33f, 4 -> 0.25f, 5 -> 0.13f )
  val spawnNumber = 4
  val spawnBabyPercentage = 0.1
}


