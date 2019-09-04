package io.scalacraft.logic.creatures

import java.util.UUID

import akka.actor.{Actor, ActorRef, Props, Timers}
import io.scalacraft.logic.commons.{DefaultTimeout, ImplicitContext}
import io.scalacraft.logic.traits.ai.general.AI
import io.scalacraft.logic.traits.creatures.{CreatureParameters, FarmAnimal, LivingBehaviour}
import io.scalacraft.packets.Entities.Sheep
import io.scalacraft.packets.clientbound.PlayPackets.{SoundCategory, SoundEffect}

class SheepActor(id: Int, UUID: UUID, x: Int, y: Int, z: Int, isBaby: Boolean, worldRef: ActorRef)
  extends Actor with Timers with ImplicitContext with DefaultTimeout with CreatureParameters[Sheep] with LivingBehaviour[Sheep] with AI[Sheep]{
  import CreatureParameters._

  val metaData = new Sheep()
  world = worldRef
  val entityId: Int = id
  val uuid: UUID = UUID
  metaData.isBaby = isBaby
  metaData.health = 8
  posX = x
  posY = y
  posZ = z
  oldPosX = x
  oldPosY = y
  oldPosZ = z
  val speed = 600 //speed u.d.m is 1/8000 block per 50ms: chicken's speed is 1.5 block/s
  val pathMovesNumber = 8

  override def receive: Receive = livingBehaviour

  override lazy val deathSoundEffect: SoundEffect = SoundEffect(metaData.deathSoundId,
    SoundCategory.Master,
    posX * SoundEffectPositionMultiplier,
    posY * SoundEffectPositionMultiplier,
    posZ * SoundEffectPositionMultiplier,
    SoundVolume,
    SoundPitch)
  override lazy val hurtSoundEffect: SoundEffect = SoundEffect(metaData.hurtSoundId,
    SoundCategory.Master,
    posX * SoundEffectPositionMultiplier,
    posY * SoundEffectPositionMultiplier,
    posZ * SoundEffectPositionMultiplier,
    SoundVolume,
    SoundPitch)

}
object SheepActor extends FarmAnimal {

  override def props(entityId: Int, UUID: UUID, x: Int, y: Int, z: Int, isBaby: Boolean, world: ActorRef): Props =
    Props(new SheepActor(entityId, UUID, x, y, z, isBaby, world))

  override def name(UUID: UUID): String = s"Sheep-$UUID"
}