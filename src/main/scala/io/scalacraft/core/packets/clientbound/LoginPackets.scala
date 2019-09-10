package io.scalacraft.core.packets.clientbound

import io.scalacraft.core.marshalling.Structure
import io.scalacraft.core.marshalling.annotations.PacketAnnotations._
import io.scalacraft.core.packets.DataTypes.{Chat, Identifier, VarInt}

object LoginPackets {

  @packet(0x00)
  case class Disconnect(reason: Chat) extends Structure

  @packet(0x01)
  case class EncryptionRequest(@maxLength(20) serverId: String,
                               @precededBy[VarInt] publicKey: Array[Byte],
                               @precededBy[VarInt] verifyTokenLength: Array[Byte]) extends Structure

  @packet(0x02)
  case class LoginSuccess(@maxLength(36) uuid: String,
                          @maxLength(16) username: String) extends Structure

  @packet(0x03)
  case class SetCompression(@boxed threshold: Int) extends Structure

  @packet(0x04)
  case class LoginPluginRequest(@boxed messageID: Int,
                                channel: Identifier,
                                data: Array[Byte]) extends Structure

}
