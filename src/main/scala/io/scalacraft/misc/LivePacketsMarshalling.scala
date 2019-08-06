package io.scalacraft.misc

import java.io.{BufferedInputStream, BufferedOutputStream, ByteArrayInputStream, ByteArrayOutputStream}

import io.scalacraft.core.{Helpers, PacketManager}

object LivePacketsMarshalling extends App {

  val skipParsingFor: List[Int] = List(0x3f, 0x52, 0x22)
  val maxPacketContentLength: Int = 256

  implicit val inStream: BufferedInputStream = new BufferedInputStream(System.in)

  val packetManager = if (args(0) == "clientbound") {
    if (args(1) == "login") {
      new PacketManager[io.scalacraft.core.clientbound.LoginPackets.type]
    } else if (args(1) == "status") {
      new PacketManager[io.scalacraft.core.clientbound.StatusPacket.type]
    } else {
      new PacketManager[io.scalacraft.core.clientbound.PlayPackets.type]
    }
  } else {
    if (args(1) == "login") {
      new PacketManager[io.scalacraft.core.serverbound.LoginPackets.type]
    } else if (args(1) == "status") {
      new PacketManager[io.scalacraft.core.serverbound.StatusPackets.type]
    } else if (args(1) == "handshaking") {
      new PacketManager[io.scalacraft.core.serverbound.HandshakingPackets.type]
    } else {
      new PacketManager[io.scalacraft.core.serverbound.PlayPackets.type]
    }
  }

  while (true) {
    var (_, length) = Helpers.readVarInt(inStream)
    val (packetIdLength, packetId) = Helpers.readVarInt(inStream)
    length -= packetIdLength

    val array = new Array[Byte](length)
    for (i <- 0 until length) {
      var b: Int = 0
      while ({ b = inStream.read(); b } < 0) { }
      array(i) = b.toByte
    }

    if (!skipParsingFor.contains(packetId)) {
      parsePacket(packetId, array)
    }
  }

  def parsePacket(packetId: Int, array: Array[Byte]): Unit = {
    val buffer = new ByteArrayInputStream(array)
    val bufferedInStream = new BufferedInputStream(buffer)
    val outArray = new ByteArrayOutputStream
    val bufferedOutStream = new BufferedOutputStream(outArray)

    try {
      val parsed = packetManager.unmarshal(packetId)(bufferedInStream)
      System.out.println(parsed)
      packetManager.marshal(parsed)(bufferedOutStream)
      bufferedOutStream.flush()

      val content = Helpers.bytes2hex(outArray.toByteArray)
      if (Helpers.bytes2hex(array) != content) {
        System.err.println("$$$ marshalling error $$$")
        printExceptionContext(packetId, array)
        System.err.println("Marshalled packet: " + content)
      }
    } catch {
      case e: Exception =>
        System.err.println("$$$ UNmarshalling error $$$")
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

    System.err.println("Packet id: " + packetId)
    System.err.println("Packet length: " + array.length)
    System.err.println("Packet content: " +  content.take(maxPacketContentLength) +
      (if (content.length > maxPacketContentLength) "<truncated>" else ""))
  }



}
