package io.scalacraft

import java.io.{BufferedInputStream, BufferedOutputStream, ByteArrayInputStream, ByteArrayOutputStream}

import io.scalacraft.core.serverbound.PlayPackets.{PlayerInfo, TestPacket}
import io.scalacraft.core.{Helpers, PacketManager}

import scala.language.postfixOps

object Entrypoint extends App {
  val pm = new PacketManager

  val originalPacket = Helpers.hex2bytes("0001e5620206432e3626b7f44ee340bcfd0a0965636961766174746100000000")
  val serializedPacket = new ByteArrayOutputStream()
  implicit val inStream: BufferedInputStream = new BufferedInputStream(new ByteArrayInputStream(originalPacket))
  implicit val outStream: BufferedOutputStream = new BufferedOutputStream(serializedPacket)



  val testPacket = pm.unmarshal(0x30).asInstanceOf[PlayerInfo]

  println(testPacket)




  pm.marshal(testPacket)

  inStream.close()
  outStream.close()

  println(serializedPacket.toByteArray.toList)

  assert(originalPacket.toList sameElements serializedPacket.toByteArray)

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
