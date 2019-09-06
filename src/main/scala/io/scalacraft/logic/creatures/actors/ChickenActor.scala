package io.scalacraft.logic.creatures.actors

import java.util.UUID

import akka.actor.{ActorRef, Props}
import io.scalacraft.logic.commons.Traits.EnrichedActor
import io.scalacraft.logic.creatures.Spawnables.SpawnableFarmAnimal
import io.scalacraft.logic.creatures.behaviours.{AI, LivingBehaviour}
import io.scalacraft.logic.creatures.parameters.CreatureParameters
import io.scalacraft.core.packets.Entities.Chicken

/**
 * The actor that represent a chicken in the game. It handle: the movement in the world (AI), the player's attack effect
 * (damage, sound and death animation) and answer to some request made by CreatureSpawner actor (i.e provides spawn and
 * despawn packages).
 * @param id the chicken id
 * @param _uuid the chicken uuid
 * @param x the chicken x position
 * @param y the chicken y position
 * @param z the chicken z position
 * @param isBaby the chicken size (baby or not)
 * @param worldRef the world actor reference
 */

class ChickenActor(id: Int, _uuid: UUID, x: Int, y: Int, z: Int, isBaby: Boolean, worldRef: ActorRef)
  extends EnrichedActor with CreatureParameters[Chicken] with LivingBehaviour[Chicken] with AI[Chicken] {

  val metaData = new Chicken
  world = worldRef
  val entityId: Int = id
  val uuid: UUID = _uuid
  metaData.isBaby = isBaby
  metaData.health = 4
  posX = x
  posY = y
  posZ = z
  oldPosX = x
  oldPosY = y
  oldPosZ = z
  val speed = 800 // speed u.d.m is 1/8000 block per 50ms: chicken's speed is 2 block/s
  val pathMovesNumber = 8

  override def receive: Receive = livingBehaviour

  override lazy val deathSoundEffectId: Int = metaData.deathSoundId

  override lazy val hurtSoundEffectId: Int = metaData.hurtSoundId

}

object ChickenActor extends SpawnableFarmAnimal {

  override def props(entityId: Int, uuid: UUID, x: Int, y: Int, z: Int, isBaby: Boolean, world: ActorRef): Props =
    Props(new ChickenActor(entityId, uuid, x, y, z, isBaby, world))

  override def name(uuid: UUID): String = s"Chicken-$uuid"

}