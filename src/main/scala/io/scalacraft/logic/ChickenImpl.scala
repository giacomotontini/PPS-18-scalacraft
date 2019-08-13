package io.scalacraft.logic

import java.util.UUID

import akka.actor.{Actor, Props}
import io.scalacraft.core.marshalling.MobsAndObjectsTypeMapping
import io.scalacraft.logic.messages.Message.GetCreatureInChunk
import io.scalacraft.logic.traits.creatures.FarmAnimal
import io.scalacraft.packets.DataTypes.Angle
import io.scalacraft.packets.Entities
import io.scalacraft.packets.Entities.Chicken
import io.scalacraft.packets.clientbound.PlayPackets.SpawnMob
import net.querz.nbt.mca.MCAUtil

class ChickenImpl(entityId: Int, UUID: UUID, x: Int, y: Int, z: Int, isBaby: Boolean)
  extends Actor {
  val metaData = new Chicken()
  metaData.isBaby = isBaby
  private val tpe = MobsAndObjectsTypeMapping.fromMobEntityClassToType(metaData.getClass)
  private val velX, velY, velZ = 0
  private val yaw, pitch, headPitch = Angle(0)
  override def receive: Receive = {
    case GetCreatureInChunk(chunkX, chunkZ) =>
      if(MCAUtil.blockToChunk(x) == chunkX && MCAUtil.blockToChunk(z) == chunkZ)
        sender ! Some(SpawnMob(entityId, UUID, tpe, x, y, z, yaw, pitch, headPitch, velX, velY, velZ, metaData))
      else sender ! None
  }
}

object ChickenImpl extends FarmAnimal {
  override def props(entityId: Int, UUID: UUID, x: Int, y: Int, z: Int, isBaby: Boolean): Props =
    Props(new ChickenImpl(entityId, UUID, x, y, z, isBaby))

  override def name(UUID: UUID): String = s"Chicken-$UUID"
}