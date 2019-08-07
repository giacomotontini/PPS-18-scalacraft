package io.scalacraft.core.fsm

import java.io.DataInputStream

import io.scalacraft.core.fsm.ConnectionState.HandshakingState
import io.scalacraft.core.network.ConnectionManager

class ConnectionController(implicit connectionManager: ConnectionManager) {
  var currentState: ConnectionState = HandshakingState()

  def handlePacket(packetId: Int, buffer: DataInputStream): Unit = {
    currentState.parsePacket(packetId, buffer)
  }

  def handleConnectionClosed(): Unit = {
    connectionManager.closeConnection()
  }
}
