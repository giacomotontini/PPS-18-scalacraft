package io.scalacraft.core.serverbound

import io.scalacraft.core.DataTypes.VarInt
import io.scalacraft.core.PacketAnnotations.{boxed, maxLength, packet, precededBy}
import io.scalacraft.core.Structure

object LoginPackets {
  @packet(0x00)
  case class LoginStart(@maxLength(16) name: String) extends Structure
  @packet(0x01)
  case class EncryptionResponse(@precededBy[VarInt] sharedSecret: List[Byte], @precededBy[VarInt] verifyToken: List[Byte]) extends Structure
  //TODO
  @packet(0x02)
  case class LoginPluginResponse(@boxed messageId: Int, successfull: Boolean/*, data: Option[List[Byte]]*/) extends Structure
}
