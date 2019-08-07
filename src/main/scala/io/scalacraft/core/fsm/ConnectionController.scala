package io.scalacraft.core.fsm

import java.io.DataInputStream

import io.scalacraft.core.fsm.ConnectionState.HandshakingState
import io.scalacraft.core.network.ConnectionManager

class ConnectionController(connectionManager: ConnectionManager) {
  var currentState: ConnectionState = HandshakingState(connectionManager)

  def handlePacket(packetId: Int, buffer: DataInputStream): Unit = {
    val newState = currentState.parsePacket(packetId, buffer)
    currentState = if(newState.isDefined && newState.get != currentState) newState.get else currentState
  }

  def handleConnectionClosed(): Unit = {
    connectionManager.closeConnection()
  }
}
