package io.scalacraft.logic.creatures.behaviours

import akka.actor.Actor
import io.scalacraft.logic.commons.Message.{AskResponse, DespawnCreature, RequestCreatureInChunk}
import io.scalacraft.logic.creatures.parameters.CreatureParameters
import io.scalacraft.core.packets.DataTypes.Angle
import io.scalacraft.core.packets.Entities.MobEntity
import io.scalacraft.core.packets.clientbound.PlayPackets.{DestroyEntities, SpawnMob}

trait BaseBehaviour[T <: MobEntity] {
  this: CreatureParameters[T] with Actor =>

  private[this] def sendResponseIfInChunk[R](chunkX: Int, chunkZ: Int, response: R): Unit =
    sender ! AskResponse(self, if (posX >> 4 == chunkX && posZ >> 4 == chunkZ) Some(response) else None)

  def baseBehaviour: Receive = {

    case RequestCreatureInChunk(chunkX, chunkZ) =>
      val spawnMobPacket = SpawnMob(entityId, uuid, tpe, posX, posY, posZ, yaw = Angle(0), pitch = Angle(0),
        headPitch = Angle(0), velocityX = 0, velocityY = 0, velocityZ = 0, metaData)
      sendResponseIfInChunk(chunkX, chunkZ, spawnMobPacket)

    case DespawnCreature(chunkX, chunkZ) =>
      sendResponseIfInChunk(chunkX, chunkZ, DestroyEntities(List(entityId)))

  }

}
