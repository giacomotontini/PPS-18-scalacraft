package io.scalacraft.misc

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, DataInputStream, DataOutputStream}

import io.scalacraft.core.marshalling.PacketManager
import io.scalacraft.packets.DataTypes.VarInt

/**
 * Tool used to deserialize packets from a flow of bytes.
 */
object LivePacketsMarshalling extends App {

  val skipParsingFor: List[Int] = List(0x3f, 0x11)
  val maxPacketContentLength: Int = 256

  implicit val inStream: DataInputStream = new DataInputStream(System.in)

  val packetManager = if (args(0) == "clientbound") {
    if (args(1) == "login") {
      new PacketManager[io.scalacraft.packets.clientbound.LoginPackets.type]
    } else if (args(1) == "status") {
      new PacketManager[io.scalacraft.packets.clientbound.StatusPacket.type]
    } else {
      new PacketManager[io.scalacraft.packets.clientbound.PlayPackets.type]
    }
  } else {
    if (args(1) == "login") {
      new PacketManager[io.scalacraft.packets.serverbound.LoginPackets.type]
    } else if (args(1) == "status") {
      new PacketManager[io.scalacraft.packets.serverbound.StatusPackets.type]
    } else if (args(1) == "handshaking") {
      new PacketManager[io.scalacraft.packets.serverbound.HandshakingPackets.type]
    } else {
      new PacketManager[io.scalacraft.packets.serverbound.PlayPackets.type]
    }
  }

  while (true) {
    var VarInt(length, _) = Helpers.readVarInt(inStream)
    val VarInt(packetId, packetIdLength) = Helpers.readVarInt(inStream)
    length -= packetIdLength


    val array = new Array[Byte](length)
    for (i <- 0 until length) {
      var b: Int = 0
      while ( {
        b = inStream.read(); b
      } < 0) {}
      array(i) = b.toByte
    }

    if (!skipParsingFor.contains(packetId)) {
      parsePacket(packetId, array)
    }
  }

  def parsePacket(packetId: Int, array: Array[Byte]): Unit = {
    val buffer = new ByteArrayInputStream(array)
    val bufferedInStream = new DataInputStream(buffer)
    val outArray = new ByteArrayOutputStream
    val bufferedOutStream = new DataOutputStream(outArray)

    try {
      val parsed = packetManager.unmarshal(packetId)(bufferedInStream)
      System.out.println(parsed)
      //      packetManager.marshal(parsed)(bufferedOutStream)
      //      bufferedOutStream.flush()
      //
      //      val content = Helpers.bytes2hex(outArray.toByteArray)
      //      if (Helpers.bytes2hex(array) != content) {
      //        System.err.println("\n$$$ marshalling error $$$")
      //        printExceptionContext(packetId, array)
      //        System.err.println("Struct content: " + content)
      //      }
    } catch {
      case e: Exception =>
        System.err.println("\n$$$ UNmarshalling error $$$")
        printExceptionContext(packetId, array)
        e.printStackTrace()
    }

    buffer.close()
    bufferedInStream.close()
    outArray.close()
    bufferedOutStream.close()
  }

  def printExceptionContext(packetId: Int, array: Array[Byte]): Unit = {
    val content = Helpers.bytes2hex(array)

    System.err.println("Packet id: 0x" + packetId.toHexString)
    System.err.println("Packet length: " + array.length)
    System.err.println("Packet content: " + content.take(128) +
      (if (content.length > maxPacketContentLength) "<truncated>" else ""))
  }

}
