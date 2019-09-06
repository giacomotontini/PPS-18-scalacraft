package io.scalacraft.logic.creatures.actors

import java.util.UUID

import akka.actor.{ActorRef, Props}
import io.scalacraft.logic.commons.Traits.EnrichedActor
import io.scalacraft.logic.creatures.Spawnables.SpawnableFarmAnimal
import io.scalacraft.logic.creatures.behaviours.{AI, LivingBehaviour}
import io.scalacraft.logic.creatures.parameters.CreatureParameters
import io.scalacraft.core.packets.Entities.Sheep

/**
 * The actor that represent a sheep in the game. It handle: the movement in the world (AI), the player's attack effect
 * (damage, sound and death animation) and answer to some request made by CreatureSpawner actor (i.e provides spawn and
 * despawn packages).
 * @param id the sheep id
 * @param _uuid the sheep uuid
 * @param x the sheep x position
 * @param y the sheep y position
 * @param z the sheep z position
 * @param isBaby the sheep size (baby or not)
 * @param worldRef the world actor reference
 */

class SheepActor(id: Int, _uuid: UUID, x: Int, y: Int, z: Int, isBaby: Boolean, worldRef: ActorRef)
  extends EnrichedActor with CreatureParameters[Sheep] with LivingBehaviour[Sheep] with AI[Sheep] {

  val metaData = new Sheep()
  world = worldRef
  val entityId: Int = id
  val uuid: UUID = _uuid
  metaData.isBaby = isBaby
  metaData.health = 8
  posX = x
  posY = y
  posZ = z
  oldPosX = x
  oldPosY = y
  oldPosZ = z
  val speed = 600 // speed u.d.m is 1/8000 block per 50ms: sheep's speed is 1.5 block/s
  val pathMovesNumber = 8

  override def receive: Receive = livingBehaviour

  override lazy val deathSoundEffectId: Int = metaData.deathSoundId

  override lazy val hurtSoundEffectId: Int = metaData.hurtSoundId

}

object SheepActor extends SpawnableFarmAnimal {

  override def props(entityId: Int, uuid: UUID, x: Int, y: Int, z: Int, isBaby: Boolean, world: ActorRef): Props =
    Props(new SheepActor(entityId, uuid, x, y, z, isBaby, world))

  override def name(uuid: UUID): String = s"Sheep-$uuid"

}