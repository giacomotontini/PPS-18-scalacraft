package io.scalacraft.core.serverbound

import java.util.UUID

import io.scalacraft.core.DataTypes.VarInt
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

  case class AddPlayer(
                        uuid: UUID,
                        @maxLength(16) name: String,
                        @precededBy[VarInt] property: Array[AddPlayerProperty],
                        @boxed gameMode: Int,
                        @boxed ping: Int,
                        @maxLength(32767) chat: Option[String]
                      ) extends PlayerInfoAction with Structure

  @packet(0x30)
  case class PlayerInfo(@switch[VarInt](VarInt(0) -> classOf[AddPlayer]) playerAction: Array[PlayerInfoAction])
    extends Structure

  @packet(0x0)
  case class TestPacket(@maxLength(16) testOption: Option[String]) extends Structure

}
