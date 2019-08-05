package io.scalacraft.core.serverbound

import io.scalacraft.core.PacketAnnotations.packet
import io.scalacraft.core.Structure

object StatusPackets {

  @packet(0x00)
  case class Request() extends Structure

  @packet(0x01)
  case class Ping(payload: Long) extends Structure

}
