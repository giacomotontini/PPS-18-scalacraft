package io.scalacraft.core.fsm

import java.io.DataInputStream

trait ParseListener {

  def parsePacket(packetId: Int, buffer: DataInputStream): Unit

}
