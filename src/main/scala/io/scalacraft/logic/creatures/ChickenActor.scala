package io.scalacraft.logic.creatures

import java.util.UUID

import akka.actor.{Actor, ActorRef, Props, Timers}
import io.scalacraft.logic.commons.{DefaultTimeout, ImplicitContext}
import io.scalacraft.logic.traits.ai.general.AI
import io.scalacraft.logic.traits.creatures.{CreatureParameters, FarmAnimal, LivingBehaviour}
import io.scalacraft.packets.Entities.Chicken
import io.scalacraft.packets.clientbound.PlayPackets
import io.scalacraft.packets.clientbound.PlayPackets.{SoundCategory, SoundEffect}

class ChickenActor(id: Int, UUID: UUID, x: Int, y: Int, z: Int, isBaby: Boolean, worldRef: ActorRef)
  extends Actor with Timers with ImplicitContext with DefaultTimeout with CreatureParameters[Chicken] with LivingBehaviour[Chicken] with AI {
  val metaData = new Chicken()
  world = worldRef
  val entityId: Int = id
  val uuid: UUID = UUID
  metaData.isBaby = isBaby
  metaData.health = 4
  posX = x
  posY = y
  posZ = z
  oldPosX = x
  oldPosY = y
  oldPosZ = z
  val speed = 800 //speed u.d.m is 1/8000 block per 50ms: chicken's speed is 2 block/s
  val pathMovesNumber = 8

  override def receive: Receive = livingBehaviour orElse aiBehaviour

  override lazy val deathSoundEffect: SoundEffect = SoundEffect(192, SoundCategory.Master, posX * 8, posY * 8, posZ * 8, 1, 0.5f)
  override lazy val hurtSoundEffect: SoundEffect = SoundEffect(195, SoundCategory.Master, posX * 8, posY * 8, posZ * 8, 1, 0.5f)
}

object ChickenActor extends FarmAnimal {

  override def props(entityId: Int, UUID: UUID, x: Int, y: Int, z: Int, isBaby: Boolean, world: ActorRef): Props =
    Props(new ChickenActor(entityId, UUID, x, y, z, isBaby, world))

  override def name(UUID: UUID): String = s"Chicken-$UUID"
}