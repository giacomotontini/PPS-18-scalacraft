package io.scalacraft.core.clientbound

import java.util.UUID

import io.scalacraft.core.DataTypes.{Angle, Slot}
import io.scalacraft.core.Entities.Entity
import io.scalacraft.core.PacketAnnotations._
import io.scalacraft.core.Structure

object PlayPackets {

  trait MobEntity{
    this: Entity =>
  }
  trait ObjectEntity{
    this: Entity =>
  }

  @packet(id = 0x03)
  case class SpawnMob(@boxed entityId: Int,
                      entityUUID: UUID,
                      @boxed tpe: Int,
                      x: Double, y: Double,
                      z: Double,  yaw: Angle,
                      pitch:  Angle,
                      headPitch: Angle,
                      velocityX: Short,
                      velocityY: Short,
                      velocityZ: Short,
                      @fromContext(2) metadata: MobEntity
                     )

  @packet(0x15)
  case class WindowItems(@byte windowId: Int, @short count:Int, slot: Slot ) extends Structure //need to check client's packet
}
