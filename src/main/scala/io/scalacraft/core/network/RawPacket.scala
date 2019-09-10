package io.scalacraft.core.network

import java.io.DataInputStream

/**
 * Represent a packet not yet parsed
 * @param packetId the packet id
 * @param payload the packet raw content in byte
 */
case class RawPacket(packetId: Int, payload: DataInputStream)
