package io.scalacraft

import java.io.{BufferedInputStream, BufferedOutputStream, ByteArrayInputStream, ByteArrayOutputStream}



import scala.language.postfixOps

object Entrypoint extends App {
  /*val pm = new PacketManager[PlayPackets.type]
  val originalPacket = Helpers.hex2bytes("57407268196e69a5a240514000")
  val serializedPacket = new ByteArrayOutputStream()
  implicit val inStream: BufferedInputStream = new BufferedInputStream(new ByteArrayInputStream(originalPacket))
  implicit val outStream: BufferedOutputStream = new BufferedOutputStream(serializedPacket)
  val testPacket = TestPacket(Direction.East)
  testPacket.productElement(0x7)
  println(VarIntMarshaller.unmarshal())
  // pm.marshal(testPacket)
  //inStream.close()
  outStream.close()
  // println(serializedPacket.toByteArray.toList)
  //assert(originalPacket.toList sameElements serializedPacket.toByteArray)*/

}
