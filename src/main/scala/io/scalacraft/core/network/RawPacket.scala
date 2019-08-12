package io.scalacraft.core.network

import java.io.DataInputStream

case class RawPacket(packetId: Int, payload: DataInputStream)
