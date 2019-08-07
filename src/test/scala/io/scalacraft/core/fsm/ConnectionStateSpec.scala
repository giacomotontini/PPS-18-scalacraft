package io.scalacraft.core.fsm

import java.io.{ByteArrayOutputStream, DataOutputStream}

import io.scalacraft.core.fsm.{ConnectionController, ConnectionState}
import io.scalacraft.core.marshalling.PacketManager
import io.scalacraft.core.network.ConnectionManager
import io.scalacraft.misc.{Helpers, ServerConfiguration}
import io.scalacraft.packets.DataTypes
import io.scalacraft.packets.DataTypes.VarInt
import io.scalacraft.packets.clientbound.StatusPacket.{Pong, Response}
import io.scalacraft.packets.serverbound.HandshakingPackets
import io.scalacraft.packets.serverbound.HandshakingPackets.Handshake
import io.scalacraft.packets.serverbound.StatusPackets.{Ping, Request}
import org.scalatest.{FlatSpec, Matchers}

import scala.util.Random

class ConnectionStateTest extends FlatSpec with Matchers {

  class DummyConnectionManager(packetId: VarInt , hexString: String) extends ConnectionManager {

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

  "A correct handshake status sequence packet" should "bring to status state" in {

    val handshakePacket = Handshake(ServerConfiguration.VERSION_PROTOCOL,
      "localhost", ServerConfiguration.PORT, HandshakingPackets.NextState.Status)

    val request = Request()
    val response = Response(ServerConfiguration.configuration)
    val payload = new Random().nextLong()
    val ping = Ping(payload)
    val pong = Pong(payload)

    val connectionManager = new DummyConnectionManager()
    val connectionController = new ConnectionController()
  }

}
