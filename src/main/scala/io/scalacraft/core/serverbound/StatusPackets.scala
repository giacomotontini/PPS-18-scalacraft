package io.scalacraft.core.serverbound

import io.scalacraft.core.PacketAnnotations.packet

object StatusPackets {

  @packet(id=0x00)
  case class Request()

  @packet(id=0x01)
  case class Ping(payload: Long)
}
