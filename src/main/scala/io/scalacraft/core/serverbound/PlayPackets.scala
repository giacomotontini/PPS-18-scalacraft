package io.scalacraft.core.serverbound

import java.util.UUID

import io.scalacraft.core.DataTypes.VarInt
import io.scalacraft.core.PacketAnnotations._
import io.scalacraft.core.Structure
import scala.reflect.runtime.universe.typeOf

object PlayPackets {

  sealed trait PlayerInfoAction {
    val uuid: UUID
  }

  case class AddPlayerProperty(name: String,
                               value: String,
                               signature: Option[String]) extends Structure

  case class AddPlayer(
                        uuid: UUID,
                        @maxLength(16) name: String,
                        @precededBy[VarInt] property: Array[AddPlayerProperty],
                        @packed gameMode: Int,
                        @packed ping: Int,
                        @maxLength(32767) chat: Option[String]
                      ) extends PlayerInfoAction with Structure


  @packet(0x30)
  case class PlayerInfo(@switch[VarInt](VarInt(0) -> typeOf[AddPlayer]) playerAction: Array[PlayerInfoAction])
    extends Structure

  @packet(0x0)
  case class TestPacket(testOption: Option[Int]) extends Structure

}
