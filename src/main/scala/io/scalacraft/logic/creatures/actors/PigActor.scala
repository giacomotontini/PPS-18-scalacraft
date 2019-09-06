package io.scalacraft.logic.creatures.actors

import java.util.UUID

import akka.actor.{ActorRef, Props}
import io.scalacraft.logic.commons.Traits.EnrichedActor
import io.scalacraft.logic.creatures.Spawnables.SpawnableFarmAnimal
import io.scalacraft.logic.creatures.behaviours.{AI, LivingBehaviour}
import io.scalacraft.logic.creatures.parameters.CreatureParameters
import io.scalacraft.core.packets.Entities.Pig
/**
 * The actor that represent a pig in the game. It handle: the movement in the world (AI), the player's attack effect
 * (damage, sound and death animation) and answer to some request made by CreatureSpawner actor (i.e provides spawn and
 * despawn packages).
 * @param id the pig id
 * @param _uuid the pig uuid
 * @param x the pig x position
 * @param y the pig y position
 * @param z the pig z position
 * @param isBaby the pig size (baby or not)
 * @param worldRef the world actor reference
 */

class PigActor(id: Int, _uuid: UUID, x: Int, y: Int, z: Int, isBaby: Boolean, worldRef: ActorRef)
  extends EnrichedActor with CreatureParameters[Pig] with LivingBehaviour[Pig] with AI[Pig] {

  val metaData = new Pig()
  world = worldRef
  val entityId: Int = id
  val uuid: UUID = _uuid
  metaData.isBaby = isBaby
  metaData.health = 10
  posX = x
  posY = y
  posZ = z
  oldPosX = x
  oldPosY = y
  oldPosZ = z
  val speed = 400 //speed u.d.m is 1/8000 block per 50ms: pig's speed is 1 block/s
  val pathMovesNumber = 6

  override def receive: Receive = livingBehaviour

  override lazy val deathSoundEffectId: Int = metaData.deathSoundId

  override lazy val hurtSoundEffectId: Int = metaData.hurtSoundId

}

object PigActor extends SpawnableFarmAnimal {

  override def props(entityId: Int, uuid: UUID, x: Int, y: Int, z: Int, isBaby: Boolean, world: ActorRef): Props =
    Props(new PigActor(entityId, uuid, x, y, z, isBaby, world))

  override def name(uuid: UUID): String = s"Pig-$uuid"

}