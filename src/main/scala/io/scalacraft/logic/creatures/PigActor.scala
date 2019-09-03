package io.scalacraft.logic.creatures

import java.util.UUID

import akka.actor.{Actor, ActorRef, Props, Timers}
import io.scalacraft.logic.commons.{DefaultTimeout, ImplicitContext}
import io.scalacraft.logic.traits.ai.general.AI
import io.scalacraft.logic.traits.creatures.{CreatureParameters, FarmAnimal, LivingBehaviour}
import io.scalacraft.packets.Entities.Pig
import io.scalacraft.packets.clientbound.PlayPackets.{SoundCategory, SoundEffect}

class PigActor(id: Int, UUID: UUID, x: Int, y: Int, z: Int, isBaby: Boolean, worldRef: ActorRef)
  extends Actor with Timers with ImplicitContext with DefaultTimeout with CreatureParameters[Pig] with LivingBehaviour[Pig] with AI[Pig] {
  import CreatureParameters._

  val metaData = new Pig()
  world = worldRef
  val entityId: Int = id
  val uuid: UUID = UUID
  metaData.isBaby = isBaby
  metaData.health = 10
  posX = x
  posY = y
  posZ = z
  oldPosX = x
  oldPosY = y
  oldPosZ = z
  val speed = 400 //speed u.d.m is 1/8000 block per 50ms: chicken's speed is 1 block/s
  val pathMovesNumber = 6

  override def receive: Receive = livingBehaviour orElse aiBehaviour

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
object PigActor extends FarmAnimal {

  override def props(entityId: Int, UUID: UUID, x: Int, y: Int, z: Int, isBaby: Boolean, world: ActorRef): Props =
    Props(new PigActor(entityId, UUID, x, y, z, isBaby, world))

  override def name(UUID: UUID): String = s"Pig-$UUID"
}