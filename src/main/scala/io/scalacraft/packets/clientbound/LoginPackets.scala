package io.scalacraft.packets.clientbound

import io.scalacraft.core.marshalling.Structure
import io.scalacraft.core.marshalling.annotations.PacketAnnotations._
import io.scalacraft.packets.DataTypes.{Chat, Identifier, VarInt}

object LoginPackets {

  @packet(0x00)
  case class Disconnect(reason: Chat) extends Structure

  @packet(0x01)
  case class EncryptionRequest(@maxLength(20) serverId: String,
                               @precededBy[VarInt] @byte publicKey: List[Int],
                               @precededBy[VarInt] @byte verifyTokenLength: List[Int]) extends Structure

  @packet(0x02)
  case class LoginSuccess(@maxLength(36) uuid: String,
                          @maxLength(16) username: String) extends Structure

  @packet(0x03)
  case class SetCompression(@boxed threshold: Int) extends Structure

  @packet(0x04)
  case class LoginPluginRequest(@boxed messageID: Int,
                                channel: Identifier,
                                @byte data: List[Int]) extends Structure

}
