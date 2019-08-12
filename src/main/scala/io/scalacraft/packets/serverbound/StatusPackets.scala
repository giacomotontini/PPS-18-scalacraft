package io.scalacraft.packets.serverbound

import io.scalacraft.core.marshalling.Structure
import io.scalacraft.core.marshalling.annotations.PacketAnnotations.packet

object StatusPackets {

  @packet(0x00)
  case class Request() extends Structure

  @packet(0x01)
  case class Ping(payload: Long) extends Structure

}
