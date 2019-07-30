package io.scalacraft.core.clientbound
import io.scalacraft.core.PacketAnnotations._

class StatusPacket {

  @packet(id=0x00)
  case class Response(@maxLength(32767) jsonResponse: String)

  @packet(id=0x01)
  case class Pong(payload: Long)
}
