package io.scalacraft.core.fsm

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, DataInputStream, DataOutputStream}
import java.util.UUID

import io.scalacraft.core.fsm.ConnectionState.{ClosedState, LoginState, StatusState}
import io.scalacraft.core.fsm.{ConnectionController, ConnectionState}
import io.scalacraft.core.marshalling.{PacketManager, Structure}
import io.scalacraft.core.network.ConnectionManager
import io.scalacraft.misc.{Helpers, ServerConfiguration}
import io.scalacraft.packets.DataTypes
import io.scalacraft.packets.DataTypes.VarInt
import io.scalacraft.packets.clientbound.LoginPackets.LoginSuccess
import io.scalacraft.packets.clientbound.StatusPacket.{Pong, Response}
import io.scalacraft.packets.serverbound.HandshakingPackets
import io.scalacraft.packets.serverbound.HandshakingPackets.Handshake
import io.scalacraft.packets.serverbound.LoginPackets.LoginStart
import io.scalacraft.packets.serverbound.StatusPackets.{Ping, Request}
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

import scala.util.Random

class ConnectionStateSpec extends FlatSpec with Matchers with BeforeAndAfter {

  var connectionManager: DummyConnectionManager = _
  var connectionController: ConnectionController = _

  before {
    connectionManager = new DummyConnectionManager()
    connectionController = new ConnectionController(connectionManager)
  }

  class DummyConnectionManager() extends ConnectionManager {

    var packetId: VarInt = _
    var hexString: String = _

    def setContext(packetId: VarInt, hexString: String): Unit = {
      this.packetId = packetId
      this.hexString = hexString
    }

    override def writePacket(dataToPacketId: DataOutputStream => DataTypes.VarInt): Unit = {
      val byteArray = new ByteArrayOutputStream()
      val os = new DataOutputStream(byteArray)
      val packetIdObtained = dataToPacketId(os)
      os.close()
      Helpers.bytes2hex(byteArray.toByteArray) shouldBe hexString
      packetIdObtained shouldBe packetId
    }

    override def closeConnection(): Unit = {}
  }

  def generateInputOutputStreams(byteArrayOutputStream: ByteArrayOutputStream): (DataOutputStream, DataInputStream) = {
    val outputStream = new DataOutputStream(byteArrayOutputStream)
    val inputStream = new DataInputStream(new ByteArrayInputStream(byteArrayOutputStream.toByteArray))
    (outputStream, inputStream)
  }

  def sendPacket(connectionController: ConnectionController, packetManager: PacketManager[_], packet: Structure ): VarInt = {
    val streams = generateInputOutputStreams(new ByteArrayOutputStream())
    val packetId = packetManager.marshal(packet)(streams._1)
    streams._1.close()
    connectionController.handlePacket(packetId.value, streams._2)
    packetId
  }

  def expectedPacket(packetManager: PacketManager[_], packet: Structure) = {
    val byteArrayOutputStream = new ByteArrayOutputStream()
    val streams = generateInputOutputStreams(byteArrayOutputStream)
    val packetId = packetManager.marshal(packet)(streams._1)
    connectionManager.setContext(packetId, Helpers.bytes2hex(byteArrayOutputStream.toByteArray))
  }


  "An handshake packet with next state 1" should " bring to a closed connection state " in {
    val handshake = Handshake(ServerConfiguration.VERSION_PROTOCOL,
      "localhost", ServerConfiguration.PORT, HandshakingPackets.NextState.Status)
    val request = Request()
    val response = Response(ServerConfiguration.configuration)
    val payload = new Random().nextLong()
    val ping = Ping(payload)
    val pong = Pong(payload)

    val handshakingServerBoundMarshaller = new PacketManager[io.scalacraft.packets.serverbound.HandshakingPackets.type]
    val statusServerBoundMarshaller = new PacketManager[io.scalacraft.packets.serverbound.StatusPackets.type]

    sendPacket(connectionController, handshakingServerBoundMarshaller, handshake)
    connectionController.currentState shouldBe StatusState

    sendPacket(connectionController, statusServerBoundMarshaller, request)
    expectedPacket(statusServerBoundMarshaller, response)
    sendPacket(connectionController, statusServerBoundMarshaller, ping)
    expectedPacket(statusServerBoundMarshaller, pong)

    connectionController.currentState shouldBe ClosedState
  }

  "An handshake packet with next state 2" should " bring to play status " in {
    val handshake = Handshake(ServerConfiguration.VERSION_PROTOCOL,
      "localhost", ServerConfiguration.PORT, HandshakingPackets.NextState.Login)
    val username = "a minecraft player"
    val loginStart = LoginStart(username)
    val loginSuccess = LoginSuccess(UUID.randomUUID.toString, username)


    val handshakingServerBoundMarshaller = new PacketManager[io.scalacraft.packets.serverbound.HandshakingPackets.type]
    val loginServerBoundMarshaller = new PacketManager[io.scalacraft.packets.serverbound.LoginPackets.type]

    sendPacket(connectionController, handshakingServerBoundMarshaller, handshake)
    connectionController.currentState shouldBe StatusState

    sendPacket(connectionController, loginServerBoundMarshaller, loginStart)
    expectedPacket(loginServerBoundMarshaller, loginSuccess)

    connectionController.currentState shouldBe LoginState
  }


}
