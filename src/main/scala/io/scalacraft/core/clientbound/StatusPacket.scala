package io.scalacraft.core.clientbound
import io.scalacraft.core.PacketAnnotations._
import io.scalacraft.core.Structure

object StatusPacket {

  @packet(id=0x00)
  case class Response(@maxLength(32767) jsonResponse: String) extends Structure

  @packet(id=0x01)
  case class Pong(payload: Long) extends Structure
}
