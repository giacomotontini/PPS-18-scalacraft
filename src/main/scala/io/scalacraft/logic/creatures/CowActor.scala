package io.scalacraft.logic.creatures

import java.util.UUID

import akka.actor.{Actor, ActorRef, Props, Timers}
import io.scalacraft.logic.commons.{DefaultTimeout, ImplicitContext}
import io.scalacraft.logic.traits.ai.general.AI
import io.scalacraft.logic.traits.creatures.{BaseBehaviour, CreatureParameters, FarmAnimal, LivingBehaviour}
import io.scalacraft.packets.Entities.Cow
import io.scalacraft.packets.clientbound.PlayPackets
import io.scalacraft.packets.clientbound.PlayPackets.{SoundCategory, SoundEffect}

class CowActor (id: Int, UUID: UUID, x: Int, y: Int, z: Int, isBaby: Boolean, worldRef: ActorRef)
  extends Actor with Timers with ImplicitContext with DefaultTimeout with CreatureParameters[Cow] with LivingBehaviour[Cow] with AI[Cow] {
  val metaData = new Cow()
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

  override lazy val deathSoundEffect: SoundEffect = SoundEffect(202, SoundCategory.Master, posX * 8, posY * 8, posZ * 8, 1, 0.5f)
  override lazy val hurtSoundEffect: SoundEffect = SoundEffect(203, SoundCategory.Master, posX * 8, posY * 8, posZ * 8, 1, 0.5f)
}
object CowActor extends FarmAnimal {

  override def props(entityId: Int, UUID: UUID, x: Int, y: Int, z: Int, isBaby: Boolean, world: ActorRef): Props =
    Props(new CowActor(entityId, UUID, x, y, z, isBaby, world))

  override def name(UUID: UUID): String = s"Cow-$UUID"
}