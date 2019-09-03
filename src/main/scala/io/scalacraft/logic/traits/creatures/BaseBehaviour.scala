package io.scalacraft.logic.traits.creatures

import akka.actor.{Actor, ActorRef}
import io.scalacraft.logic.commons.Message.{AskResponse, DespawnCreature, RequestCreatureInChunk}
import io.scalacraft.packets.DataTypes.Angle
import io.scalacraft.packets.Entities.MobEntity
import io.scalacraft.packets.clientbound.PlayPackets.{DestroyEntities, SpawnMob}
import net.querz.nbt.mca.MCAUtil

trait BaseBehaviour[T <: MobEntity] {
  this: CreatureParameters[T] with Actor =>

  private[this] def sendAnOptionAskResponseIfIsMyChunk[T](chunkX: Int, chunkZ: Int, sender: ActorRef, response: T): Unit = {
    def isMyChunk(chunkX: Int, chunkZ: Int): Boolean = {
      MCAUtil.blockToChunk(posX) == chunkX && MCAUtil.blockToChunk(posZ) == chunkZ
    }

    if (isMyChunk(chunkX, chunkZ))
      sender ! AskResponse(self, Some(response))
    else
      sender ! AskResponse(self, None)

  }

  def baseBehaviour: Receive = {
    case RequestCreatureInChunk(chunkX, chunkZ) =>
      val spawnMobPacket = SpawnMob(entityId, uuid, tpe, posX, posY, posZ, yaw = Angle(0), pitch = Angle(0), headPitch = Angle(0),
        velocityX = 0, velocityY = 0, velocityZ = 0, metaData)
      sendAnOptionAskResponseIfIsMyChunk[SpawnMob](chunkX, chunkZ, sender, spawnMobPacket)
    case DespawnCreature(chunkX, chunkZ) =>
      val destroyEntityPacket = DestroyEntities(List(entityId))
      sendAnOptionAskResponseIfIsMyChunk[DestroyEntities](chunkX, chunkZ, sender, destroyEntityPacket)
  }

}
