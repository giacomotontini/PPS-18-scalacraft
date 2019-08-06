package io.scalacraft.core

import java.io.{BufferedInputStream, DataInputStream, DataOutputStream}
import java.util.UUID

import io.scalacraft.core.clientbound.LoginPackets.LoginSuccess
import io.scalacraft.core.clientbound.StatusPacket.{Pong, Response}

class PacketDispatcher {

  trait ConnectionManager {
    def writePacket(dataToPacketId: DataOutputStream => Int): Unit

    def closeConnection(): Unit
  }


  class ConnectionController(implicit connectionManager: ConnectionManager) {
    var currentState: ConnectionState = HandshakingState()

    def handlePacket(packetId: Int, buffer: DataInputStream): Unit = {
      currentState.parsePacket(packetId, buffer)
    }

    def handleConnectionClosed(): Unit = {
      connectionManager.closeConnection()
    }
  }

  trait ConnectionState {
    val connectionManager: ConnectionManager
    val packetManagerClientBound: PacketManager[_]

    def writePacket(packet: Structure) = {
      //connectionManager.writePacket(dataOutputStream => packetManagerClientBound.marshal(packet)(dataOutputStream))
    }

    def parsePacket(packetId: Int, buffer: DataInputStream): Option[ConnectionState]

    def notifyConnectionClosed(): Unit
  }

  case class HandshakingState(implicit val connectionManager: ConnectionManager) extends ConnectionState {

    val packetManagerClientBound = null
    val packetManagerServerBound = new PacketManager[io.scalacraft.core.serverbound.StatusPackets.type]

    override def parsePacket(packetId: Int, buffer: DataInputStream): Option[ConnectionState] = packetManagerServerBound.unmarshal(packetId)(buffer) match {
      case handshake: serverbound.HandshakingPackets.Handshake if handshake.nextState == 1 => Some(StatusState())
      case handshake: serverbound.HandshakingPackets.Handshake if handshake.nextState == 2 => Some(LoginState())
      case _ => {
        System.err.println("[HandshakingState] Unhandled packet with id:", packetId)
        None
      }
    }

    override def notifyConnectionClosed(): Unit = {
      connectionManager.closeConnection()
    }

  }

  case class StatusState(implicit val connectionManager: ConnectionManager) extends ConnectionState {

    val packetManagerServerBound = new PacketManager[io.scalacraft.core.serverbound.StatusPackets.type]
    val packetManagerClientBound = new PacketManager[io.scalacraft.core.clientbound.StatusPacket.type]
    var requestReceived: Boolean = false

    override def parsePacket(packetId: Int, buffer: DataInputStream): Option[ConnectionState] = packetManagerServerBound.unmarshal(packetId)(buffer) match {
      case _: serverbound.StatusPackets.Request => {
        requestReceived = true
        println("[StatusState] Request received")
        writePacket(Response(ServerConfiguration.configuration))
        None
      }
      case ping: serverbound.StatusPackets.Ping => {
        if (requestReceived) {
          val pong = Pong(ping.payload)
          writePacket(pong)
        } else {
          System.err.println("Received a ping withount a previous Request")
        }
        Some(ClosedState())
      }
      case _ => {
        System.err.println("Unhandled packet with id: ", packetId, " within LoginState")
        None
      }
    }

    override def notifyConnectionClosed(): Unit = {
      connectionManager.closeConnection()
    }
  }

  case class LoginState(implicit val connectionManager: ConnectionManager) extends ConnectionState {

    val packetManagerServerBound = new PacketManager[io.scalacraft.core.serverbound.LoginPackets.type]
    val packetManagerClientBound = new PacketManager[io.scalacraft.core.clientbound.LoginPackets.type]

    var loginStartReceived: Boolean = false

    override def parsePacket(packetId: Int, buffer: DataInputStream): Option[ConnectionState] = packetManagerServerBound.unmarshal(packetId)(buffer) match {
      case loginStart: serverbound.LoginPackets.LoginStart => {
        println("[LoginState] LoginStarted for " + loginStart.name)
        val uuid = UUID.randomUUID().toString
        val loginSuccess = LoginSuccess(uuid, loginStart.name)
        //skipping setCompression and Encryption: Not supported
        writePacket(loginSuccess)
        None
      }
      case _ => {
        System.err.println("Unhandled packet with id: ", packetId, " within LoginState")
        None
      }
    }

    override def notifyConnectionClosed(): Unit = {
      connectionManager.closeConnection()
    }
  }

  case class PlayState(implicit val connectionManager: ConnectionManager) extends ConnectionState {

    val packetManagerServerBound = new PacketManager[io.scalacraft.core.serverbound.PlayPackets.type]
    val packetManagerClientBound = new PacketManager[io.scalacraft.core.clientbound.PlayPackets.type]

    override def parsePacket(packetId: Int, buffer: DataInputStream): Option[ConnectionState] = packetManagerServerBound.unmarshal(packetId)(buffer) match {
      case _ => {
        System.err.println("Unhandled packet with id: ", packetId, " within PlayState")
        None
      }
    }

    override def notifyConnectionClosed(): Unit = {
      connectionManager.closeConnection()
    }
  }


  case class ClosedState(implicit val connectionManager: ConnectionManager) extends ConnectionState {
    val packetManagerClientBound = null //not used

    override def parsePacket(packetId: Int, buffer: DataInputStream): Option[ConnectionState] = {
      println("Connection closed. Can't parse packet with id: ", packetId)
      None
    }

    override def notifyConnectionClosed(): Unit = {}
  }

}