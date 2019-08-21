package io.scalacraft.logic

import java.util.UUID

import akka.actor.{Actor, ActorRef, Props, Timers}
import io.scalacraft.logic.messages.Message.{AskResponse, DespawnCreature, GetCreatureInChunk}
import io.scalacraft.logic.traits.ai.ChickenAI
import io.scalacraft.logic.traits.creatures.{CreatureParameters, FarmAnimal}
import io.scalacraft.logic.traits.{DefaultTimeout, ImplicitContext}
import io.scalacraft.packets.DataTypes.Angle
import io.scalacraft.packets.clientbound.PlayPackets._
import net.querz.nbt.mca.MCAUtil

class ChickenActor(id: Int, UUID: UUID, x: Int, y: Int, z: Int, isBaby: Boolean, worldRef: ActorRef)
  extends Actor with Timers with ImplicitContext with DefaultTimeout with CreatureParameters with ChickenAI {
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

  private[this] def sendAnOptionAskResponseIfIsMyChunk[T](chunkX: Int, chunkZ: Int, sender: ActorRef, response: T): Unit = {
    def isMyChunk(chunkX: Int, chunkZ: Int): Boolean = {
      MCAUtil.blockToChunk(x) == chunkX && MCAUtil.blockToChunk(z) == chunkZ
    }

    if (isMyChunk(chunkX, chunkZ))
      sender ! AskResponse(self, Some(response))
    else
      sender ! AskResponse(self, None)

  }


  def baseBehaviour: Receive = {
    case GetCreatureInChunk(chunkX, chunkZ) =>
      val spawnMobPacket = SpawnMob(entityId, UUID, tpe, x, y, z, yaw = Angle(0), pitch = Angle(0), headPitch = Angle(0),
        velocityX = 0, velocityY = 0, velocityZ = 0, metaData)
      sendAnOptionAskResponseIfIsMyChunk[SpawnMob](chunkX, chunkZ, sender, spawnMobPacket)
    case DespawnCreature(chunkX, chunkZ) =>
      val destroyEntityPacket = DestroyEntities(List(entityId))
      sendAnOptionAskResponseIfIsMyChunk[DestroyEntities](chunkX, chunkZ, sender, destroyEntityPacket)
  }

  override def receive: Receive = baseBehaviour orElse (aiBehaviour)
}

object ChickenActor extends FarmAnimal {

  override def props(entityId: Int, UUID: UUID, x: Int, y: Int, z: Int, isBaby: Boolean, world: ActorRef): Props =
    Props(new ChickenActor(entityId, UUID, x, y, z, isBaby, world))

  override def name(UUID: UUID): String = s"Chicken-$UUID"
}