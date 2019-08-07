package io.scalacraft.core.fsm

import java.io.DataInputStream
import java.util.UUID

import io.scalacraft.core.marshalling.{PacketManager, Structure}
import io.scalacraft.core.network.ConnectionManager
import io.scalacraft.misc.ServerConfiguration
import io.scalacraft.packets.clientbound.LoginPackets.LoginSuccess
import io.scalacraft.packets.clientbound.StatusPacket.{Pong, Response}

trait ConnectionState {
  val connectionManager: ConnectionManager
  val packetManagerClientBound: PacketManager[_]

  def writePacket(packet: Structure): Unit = {
    //connectionManager.writePacket(dataOutputStream => packetManagerClientBound.marshal(packet)(dataOutputStream))
  }

  def parsePacket(packetId: Int, buffer: DataInputStream): Option[ConnectionState]

  def notifyConnectionClosed(): Unit
}

object ConnectionState {

  case class HandshakingState(implicit val connectionManager: ConnectionManager) extends ConnectionState {

    val packetManagerClientBound = null
    val packetManagerServerBound = new PacketManager[io.scalacraft.packets.serverbound.StatusPackets.type]

    override def parsePacket(packetId: Int, buffer: DataInputStream): Option[ConnectionState] = packetManagerServerBound.unmarshal(packetId)(buffer) match {
      case handshake: io.scalacraft.packets.serverbound.HandshakingPackets.Handshake if handshake.nextState == 1 => Some(StatusState())
      case handshake: io.scalacraft.packets.serverbound.HandshakingPackets.Handshake if handshake.nextState == 2 => Some(LoginState())
      case _ =>
        System.err.println("[HandshakingState] Unhandled packet with id:", packetId)
        None
    }

    override def notifyConnectionClosed(): Unit = {
      connectionManager.closeConnection()
    }

  }

  case class StatusState(implicit val connectionManager: ConnectionManager) extends ConnectionState {

    val packetManagerServerBound = new PacketManager[io.scalacraft.packets.serverbound.StatusPackets.type]
    val packetManagerClientBound = new PacketManager[io.scalacraft.packets.clientbound.StatusPacket.type]
    var requestReceived: Boolean = false

    override def parsePacket(packetId: Int, buffer: DataInputStream): Option[ConnectionState] = packetManagerServerBound.unmarshal(packetId)(buffer) match {
      case _: io.scalacraft.packets.serverbound.StatusPackets.Request =>
        requestReceived = true
        println("[StatusState] Request received")
        writePacket(Response(ServerConfiguration.configuration))
        None
      case ping: io.scalacraft.packets.serverbound.StatusPackets.Ping =>
        if (requestReceived) {
          val pong = Pong(ping.payload)
          writePacket(pong)
        } else {
          System.err.println("Received a ping withount a previous Request")
        }
        Some(ClosedState())
      case _ =>
        System.err.println("Unhandled packet with id: ", packetId, " within LoginState")
        None
    }

    override def notifyConnectionClosed(): Unit = {
      connectionManager.closeConnection()
    }
  }

  case class LoginState(implicit val connectionManager: ConnectionManager) extends ConnectionState {

    val packetManagerServerBound = new PacketManager[io.scalacraft.packets.serverbound.LoginPackets.type]
    val packetManagerClientBound = new PacketManager[io.scalacraft.packets.clientbound.LoginPackets.type]

    var loginStartReceived: Boolean = false

    override def parsePacket(packetId: Int, buffer: DataInputStream): Option[ConnectionState] = packetManagerServerBound.unmarshal(packetId)(buffer) match {
      case loginStart: io.scalacraft.packets.serverbound.LoginPackets.LoginStart =>
        println("[LoginState] LoginStarted for " + loginStart.name)
        val uuid = UUID.randomUUID().toString
        val loginSuccess = LoginSuccess(uuid, loginStart.name)
        //skipping setCompression and Encryption: Not supported
        writePacket(loginSuccess)
        None
      case _ =>
        System.err.println("Unhandled packet with id: ", packetId, " within LoginState")
        None
    }

    override def notifyConnectionClosed(): Unit = {
      connectionManager.closeConnection()
    }
  }

  case class PlayState(implicit val connectionManager: ConnectionManager) extends ConnectionState {

    val packetManagerServerBound = new PacketManager[io.scalacraft.packets.serverbound.PlayPackets.type]
    val packetManagerClientBound = new PacketManager[io.scalacraft.packets.clientbound.PlayPackets.type]

    override def parsePacket(packetId: Int, buffer: DataInputStream): Option[ConnectionState] = packetManagerServerBound.unmarshal(packetId)(buffer) match {
      case _ =>
        System.err.println("Unhandled packet with id: ", packetId, " within PlayState")
        None
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
