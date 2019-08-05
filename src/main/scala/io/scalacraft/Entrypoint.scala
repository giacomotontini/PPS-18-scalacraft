package io.scalacraft

import java.io.{BufferedInputStream, ByteArrayInputStream}

import io.scalacraft.core.Marshallers.VarIntMarshaller
import io.scalacraft.core.{Context, Helpers, PacketManager}

import scala.language.postfixOps

object Entrypoint extends App {

  implicit val inStream: BufferedInputStream = new BufferedInputStream(System.in)
  implicit val context: Context = Context.trash

  val marshaller = new VarIntMarshaller()

  val packetManager = if (args(0) == "server") {
    new PacketManager[io.scalacraft.core.clientbound.PlayPackets.type]
  } else {
    new PacketManager[io.scalacraft.core.serverbound.PlayPackets.type]
  }

  while (true) {
    val length = marshaller.unmarshal().asInstanceOf[Int] - 1
    val packetId = marshaller.unmarshal().asInstanceOf[Int]
    val array = new Array[Byte](length)
    for (i <- 0 until length) {
      array(i) = inStream.read().toByte
    }

    val buffer = new ByteArrayInputStream(array)
    val bufferedStream = new BufferedInputStream(buffer)

    try {
      val parsed = packetManager.unmarshal(packetId)(bufferedStream)
      println(parsed)
    } catch {
      case e: Exception =>
        if (packetId != 82 && packetId != 63) {
          System.err.println("packet id " + packetId)
          e.printStackTrace()
          System.err.println(Helpers.bytes2hex(array))
        }

    }

    buffer.close()
    bufferedStream.close()
  }

}
