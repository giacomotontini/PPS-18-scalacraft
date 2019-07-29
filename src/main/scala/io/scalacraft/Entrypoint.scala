package io.scalacraft

import java.io.{BufferedInputStream, BufferedOutputStream, ByteArrayInputStream, ByteArrayOutputStream}

import io.scalacraft.core.serverbound.PlayPackets
import io.scalacraft.core.serverbound.PlayPackets.TestPacket
import io.scalacraft.core.{Helpers, PacketManager}

import scala.language.postfixOps

object Entrypoint extends App {

  val pm = new PacketManager[PlayPackets.type]

  val originalPacket = Helpers.hex2bytes("0100000005")
  val serializedPacket = new ByteArrayOutputStream()
  implicit val inStream: BufferedInputStream = new BufferedInputStream(new ByteArrayInputStream(originalPacket))
  implicit val outStream: BufferedOutputStream = new BufferedOutputStream(serializedPacket)

  val testPacket = pm.unmarshal(0).asInstanceOf[TestPacket]
  println(testPacket)

  inStream.close()
  outStream.close()

//  assert(originalPacket.toList sameElements serializedPacket.toByteArray)

  //  case class Test(value: Int)
  //  val t = Test(42)
  //  val field = t.getClass.getDeclaredField("value")
  //  field.setAccessible(true)
  //  val clazz = Class.forName(classOf[Test].getName)
  //  val ctor = clazz.getConstructor(classOf[Int])
  //  val y = ctor.newInstance(42)
  //  println(y)
  //
  //    val time = System.currentTimeMillis()
  //    for (i <- 0 to 1000000) {
  //
  //      val y = ctor.newInstance(42)
  //
  //    }
  //    println(System.currentTimeMillis() - time)

}
