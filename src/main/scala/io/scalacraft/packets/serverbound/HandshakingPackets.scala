package io.scalacraft.packets.serverbound

import io.scalacraft.core.marshalling.Structure
import io.scalacraft.core.marshalling.annotations.PacketAnnotations._
import io.scalacraft.packets.DataTypes.VarInt

object HandshakingPackets {

  sealed trait NextState
  object NextState {
    @enumValue(1) case object Status extends NextState
    @enumValue(2) case object Login extends NextState
  }

  @packet(0x00)
  case class Handshake(@boxed protocolVersion: Int,
                       @maxLength(255) serverAddress: String,
                       @short serverPort:Int,
                       @enumType[VarInt] nextState: NextState) extends Structure

  //payload should be always 0x01
  @packet(0xFE)
  case class LegacyServerListPing(@byte payload: Int) extends Structure

}
