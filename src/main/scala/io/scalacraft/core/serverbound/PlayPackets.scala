package io.scalacraft.core.serverbound

import java.util.UUID

import io.scalacraft.core.DataTypes.{Particle, VarInt}
import io.scalacraft.core.PacketAnnotations._
import io.scalacraft.core.Structure

object PlayPackets {

  sealed trait PlayerInfoAction {
    val uuid: UUID
  }

  case class AddPlayerProperty(name: String,
                               @boxed test: Option[Int],
                               value: String,
                               signature: Option[String]) extends Structure
  @switchKey(0)
  case class AddPlayer(
                        uuid: UUID,
                        @maxLength(16) name: String,
                        @precededBy[VarInt] property: Array[AddPlayerProperty],
                        @boxed gameMode: Int,
                        @boxed ping: Int,
                        @maxLength(32767) chat: Option[String]
                      ) extends PlayerInfoAction with Structure

  
  sealed trait Direction
  object Direction {
    @enumValue(2) case object North extends Direction
    @enumValue(1) case object East extends Direction
  }

  @packet(0x30)
  case class PlayerInfo(@switchType[VarInt] @precededBy[VarInt] playerAction: Array[PlayerInfoAction])
    extends Structure

  @packet(0x00)
  case class TestPacket(particle: Particle) extends Structure

}
