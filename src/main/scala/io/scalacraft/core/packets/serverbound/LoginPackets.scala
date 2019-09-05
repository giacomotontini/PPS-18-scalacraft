package io.scalacraft.core.packets.serverbound

import io.scalacraft.core.marshalling.Structure
import io.scalacraft.core.marshalling.annotations.PacketAnnotations._
import io.scalacraft.core.packets.DataTypes.VarInt

object LoginPackets {

  @packet(0x00)
  case class LoginStart(@maxLength(16) name: String) extends Structure

  @packet(0x01)
  case class EncryptionResponse(@precededBy[VarInt] sharedSecret: List[Byte],
                                @precededBy[VarInt] verifyToken: List[Byte]) extends Structure

  @packet(0x02)
  case class LoginPluginResponse(@boxed messageId: Int,
                                 successful: Boolean,
                                 data: Array[Byte]) extends Structure

}
