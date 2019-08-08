package io.scalacraft.core.fsm

import java.io.DataInputStream
import java.util.UUID

import io.scalacraft.Entrypoint
import io.scalacraft.core.marshalling.{PacketManager, Structure}
import io.scalacraft.core.network.ConnectionManager
import io.scalacraft.logic.messages.Message.PlayerLogged
import io.scalacraft.misc.ServerConfiguration
import io.scalacraft.packets.clientbound.LoginPackets.LoginSuccess
import io.scalacraft.packets.clientbound.StatusPacket.{Pong, Response}
import io.scalacraft.packets.serverbound.HandshakingPackets.NextState

trait ConnectionState {
  val connectionManager: ConnectionManager
  val packetManagerClientBound: PacketManager[_]

  def writePacket(packet: Structure): Unit = {
    connectionManager.writePacket(dataOutputStream => packetManagerClientBound.marshal(packet)(dataOutputStream))
  }

  def parsePacket(packetId: Int, buffer: DataInputStream): Option[ConnectionState]

  def notifyConnectionClosed(): Unit
}

object ConnectionState {

  case class HandshakingState(connectionManager: ConnectionManager) extends ConnectionState {

    val packetManagerClientBound: PacketManager[_] = null
    val packetManagerServerBound = new PacketManager[io.scalacraft.packets.serverbound.HandshakingPackets.type]

    override def parsePacket(packetId: Int, buffer: DataInputStream): Option[ConnectionState] = packetManagerServerBound.unmarshal(packetId)(buffer) match {
      case handshake: io.scalacraft.packets.serverbound.HandshakingPackets.Handshake if handshake.nextState == NextState.Status => Some(StatusState(connectionManager))
      case handshake: io.scalacraft.packets.serverbound.HandshakingPackets.Handshake if handshake.nextState == NextState.Login => Some(LoginState(connectionManager))
      case p => println(p)
        System.err.println("[HandshakingState] Unhandled packet with id:", packetId)
        None
    }

    override def notifyConnectionClosed(): Unit = {
      connectionManager.closeConnection()
    }

  }

  case class StatusState(connectionManager: ConnectionManager) extends ConnectionState {

    val packetManagerClientBound = new PacketManager[io.scalacraft.packets.clientbound.StatusPacket.type]
    val packetManagerServerBound = new PacketManager[io.scalacraft.packets.serverbound.StatusPackets.type]
    var requestReceived: Boolean = false

    override def parsePacket(packetId: Int, buffer: DataInputStream): Option[ConnectionState] = packetManagerServerBound.unmarshal(packetId)(buffer) match {
      case _: io.scalacraft.packets.serverbound.StatusPackets.Request =>
        requestReceived = true
        println("[StatusState] Request received")
        writePacket(Response(ServerConfiguration.configuration))
        None
      case ping: io.scalacraft.packets.serverbound.StatusPackets.Ping =>
        println("Ping received")
        if (requestReceived) {
          val pong = Pong(ping.payload)
          writePacket(pong)
        } else {
          throw new IllegalStateException("Received a ping without a previous Request")
        }
        Some(ClosedState(connectionManager))
      case _ =>
        System.err.println("Unhandled packet with id: ", packetId, " within LoginState")
        None
    }

    override def notifyConnectionClosed(): Unit = {
      connectionManager.closeConnection()
    }
  }

  case class LoginState(connectionManager: ConnectionManager) extends ConnectionState {

    val packetManagerClientBound = new PacketManager[io.scalacraft.packets.clientbound.LoginPackets.type]
    val packetManagerServerBound = new PacketManager[io.scalacraft.packets.serverbound.LoginPackets.type]

    var uuidGenerator= () => UUID.randomUUID()

    override def parsePacket(packetId: Int, buffer: DataInputStream): Option[ConnectionState] = packetManagerServerBound.unmarshal(packetId)(buffer) match {
      case loginStart: io.scalacraft.packets.serverbound.LoginPackets.LoginStart =>
        println("[LoginState] LoginStarted for " + loginStart.name)
        val loginSuccess = LoginSuccess(uuidGenerator().toString, loginStart.name)
        //skipping setCompression and Encryption: Not supported
        writePacket(loginSuccess)
        Some(PlayState(connectionManager))
      case _ =>
        System.err.println("Unhandled packet with id: ", packetId, " within LoginState")
        None
    }

    override def notifyConnectionClosed(): Unit = {
      connectionManager.closeConnection()
    }
  }

  case class PlayState(connectionManager: ConnectionManager) extends ConnectionState {

    val packetManagerClientBound = new PacketManager[io.scalacraft.packets.clientbound.PlayPackets.type]
    val packetManagerServerBound = new PacketManager[io.scalacraft.packets.serverbound.PlayPackets.type]
    var listener: ParseListener = _

    // TODO: TODO: debito tecnico

    Entrypoint.world ! PlayerLogged(this)

    def attachListener(parseListener: ParseListener): Unit = listener = parseListener

    override def parsePacket(packetId: Int, buffer: DataInputStream): Option[ConnectionState] = {
      listener.parsePacket(packetId, buffer)
      None
    }

    override def notifyConnectionClosed(): Unit = {
      connectionManager.closeConnection()
    }
  }

  case class ClosedState(connectionManager: ConnectionManager) extends ConnectionState {
    val packetManagerClientBound: PacketManager[_] = null //not used

    override def parsePacket(packetId: Int, buffer: DataInputStream): Option[ConnectionState] = {
      println("Connection closed. Can't parse packet with id: ", packetId)
      None
    }

    override def notifyConnectionClosed(): Unit = {}
  }

}
