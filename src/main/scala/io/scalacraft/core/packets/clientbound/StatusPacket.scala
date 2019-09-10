package io.scalacraft.core.packets.clientbound

import io.scalacraft.core.marshalling.Structure
import io.scalacraft.core.marshalling.annotations.PacketAnnotations._

object StatusPacket {

  @packet(0x00)
  case class Response(jsonResponse: String) extends Structure

  @packet(0x01)
  case class Pong(payload: Long) extends Structure

}
