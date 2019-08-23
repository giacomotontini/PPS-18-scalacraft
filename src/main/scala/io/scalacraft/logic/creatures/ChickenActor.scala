package io.scalacraft.logic.creatures

import java.util.UUID

import akka.actor.{Actor, ActorRef, Props, Timers}
import io.scalacraft.core.marshalling.MobsAndObjectsTypeMapping
import io.scalacraft.logic.traits.ai.general.AI
import io.scalacraft.logic.traits.creatures.{BaseBehaviour, CreatureParameters, FarmAnimal}
import io.scalacraft.logic.traits.{DefaultTimeout, ImplicitContext}
import io.scalacraft.packets.Entities.Chicken

class ChickenActor(id: Int, UUID: UUID, x: Int, y: Int, z: Int, isBaby: Boolean, worldRef: ActorRef)
  extends Actor with Timers with ImplicitContext with DefaultTimeout with CreatureParameters with BaseBehaviour with AI {
  val metaData = new Chicken()
  world = worldRef
  val entityId: Int = id
  val uuid: UUID = UUID
  metaData.isBaby = isBaby
  posX = x
  posY = y
  posZ = z
  oldPosX = x
  oldPosY = y
  oldPosZ = z
  val speed = 800 //speed u.d.m is 1/8000 block per 50ms: chicken's speed is 2 block/s
  val pathMovesNumber = 8

  override def receive: Receive = baseBehaviour orElse aiBehaviour
}

object ChickenActor extends FarmAnimal {

  override def props(entityId: Int, UUID: UUID, x: Int, y: Int, z: Int, isBaby: Boolean, world: ActorRef): Props =
    Props(new ChickenActor(entityId, UUID, x, y, z, isBaby, world))

  override def name(UUID: UUID): String = s"Chicken-$UUID"
}