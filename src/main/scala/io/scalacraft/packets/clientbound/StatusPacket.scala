package io.scalacraft.packets.clientbound
import io.scalacraft.core.marshalling.Structure
import io.scalacraft.core.marshalling.annotations.PacketAnnotations._

object StatusPacket {

  @packet(0x00)
  case class Response(@maxLength(32767) jsonResponse: String) extends Structure

  @packet(0x01)
  case class Pong(payload: Long) extends Structure

}
