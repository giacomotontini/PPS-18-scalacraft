package io.scalacraft.core.clientbound

import io.scalacraft.core.DataTypes.Slot
import io.scalacraft.core.PacketAnnotations.{byte, packet, short}
import io.scalacraft.core.Structure

object PlayPackets {

  @packet(0x15)
  case class WindowItems(@byte windowId: Int, @short count:Int, slot: Slot ) extends Structure //need to check client's packet
}
