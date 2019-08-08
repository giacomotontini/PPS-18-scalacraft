package io.scalacraft.core.fsm

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, DataInputStream, DataOutputStream}
import java.util.UUID

import io.scalacraft.core.fsm.ConnectionState.{ClosedState, LoginState, PlayState, StatusState}
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
import org.scalatest.{BeforeAndAfter, BeforeAndAfterEach, FlatSpec, Matchers}

import scala.util.Random

class ConnectionStateSpec extends FlatSpec with Matchers with BeforeAndAfterEach{

  var connectionManager: DummyConnectionManager = _
  var connectionController: ConnectionController = _

  override def beforeEach() = {
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

  def sendPacket(connectionController: ConnectionController, packetManager: PacketManager[_], packet: Structure ): VarInt = {
    val os = new ByteArrayOutputStream()
    val dataOutputStream = new DataOutputStream(os)
    val packetId = packetManager.marshal(packet)(dataOutputStream)
    dataOutputStream.close()
    val dataInputStream = new DataInputStream(new ByteArrayInputStream(os.toByteArray))
    connectionController.handlePacket(packetId.value, dataInputStream)
    packetId
  }

  def expectedPacket(packetManager: PacketManager[_], packet: Structure) = {
    val os = new ByteArrayOutputStream()
    val dataOutputStream = new DataOutputStream(os)
    val packetId = packetManager.marshal(packet)(dataOutputStream)
    dataOutputStream.close()
    connectionManager.setContext(packetId, Helpers.bytes2hex(os.toByteArray))
  }



  "An handshake packet with next state 1" should " bring to a closed connection state " in {
    val handshake = Handshake(ServerConfiguration.VersionProtocol,
      "localhost", ServerConfiguration.Port, HandshakingPackets.NextState.Status)
    val request = Request()
    val response = Response(ServerConfiguration.configuration)
    val payload = new Random().nextLong()
    val ping = Ping(payload)
    val pong = Pong(payload)

    val handshakingServerBoundMarshaller = new PacketManager[io.scalacraft.packets.serverbound.HandshakingPackets.type]
    val statusServerBoundMarshaller = new PacketManager[io.scalacraft.packets.serverbound.StatusPackets.type]
    val statusClientBoundMarshaller = new PacketManager[io.scalacraft.packets.clientbound.StatusPacket.type]

    sendPacket(connectionController, handshakingServerBoundMarshaller, handshake)
    connectionController.currentState.isInstanceOf[StatusState] shouldBe true

    expectedPacket(statusClientBoundMarshaller, response)
    sendPacket(connectionController, statusServerBoundMarshaller, request)
    expectedPacket(statusClientBoundMarshaller, pong)
    sendPacket(connectionController, statusServerBoundMarshaller, ping)

    connectionController.currentState.isInstanceOf[ClosedState] shouldBe true
  }


   it should "throw IllegalStateException if a ping is received before a Request" in {
    val handshake = Handshake(ServerConfiguration.VersionProtocol,
      "localhost", ServerConfiguration.Port, HandshakingPackets.NextState.Status)
    val request = Request()
    val response = Response(ServerConfiguration.configuration)
    val payload = new Random().nextLong()
    val ping = Ping(payload)
    val pong = Pong(payload)

    val handshakingServerBoundMarshaller = new PacketManager[io.scalacraft.packets.serverbound.HandshakingPackets.type]
    val statusServerBoundMarshaller = new PacketManager[io.scalacraft.packets.serverbound.StatusPackets.type]
    val statusClientBoundMarshaller = new PacketManager[io.scalacraft.packets.clientbound.StatusPacket.type]

    sendPacket(connectionController, handshakingServerBoundMarshaller, handshake)
    connectionController.currentState.isInstanceOf[StatusState] shouldBe true
    expectedPacket(statusClientBoundMarshaller, pong)

    intercept[IllegalStateException] {
      sendPacket(connectionController, statusServerBoundMarshaller, ping)
    }
  }

  "An handshake packet with next state 2" should " bring to play status " in {
    val handshake = Handshake(ServerConfiguration.VersionProtocol,
      "localhost", ServerConfiguration.Port, HandshakingPackets.NextState.Login)
    val username = "a player"
    val myUUID = UUID.randomUUID()
    val loginStart = LoginStart(username)
    val loginSuccess = LoginSuccess(myUUID.toString, username)
    val customUuidGenerator = () => myUUID

    val handshakingServerBoundMarshaller = new PacketManager[io.scalacraft.packets.serverbound.HandshakingPackets.type]
    val loginServerBoundMarshaller = new PacketManager[io.scalacraft.packets.serverbound.LoginPackets.type]
    val loginClientBoundMarshaller = new PacketManager[io.scalacraft.packets.clientbound.LoginPackets.type]

    sendPacket(connectionController, handshakingServerBoundMarshaller, handshake)
    connectionController.currentState.isInstanceOf[LoginState] shouldBe true

    val loginState =  connectionController.currentState.asInstanceOf[LoginState]
    loginState.uuidGenerator = customUuidGenerator

    expectedPacket(loginClientBoundMarshaller, loginSuccess)
    sendPacket(connectionController, loginServerBoundMarshaller, loginStart)

    connectionController.currentState.isInstanceOf[PlayState] shouldBe true
  }

}
