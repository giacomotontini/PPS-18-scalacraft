package io.scalacraft

import java.io.{BufferedInputStream, BufferedOutputStream, ByteArrayInputStream, ByteArrayOutputStream}

import io.scalacraft.core.DataTypes.SlotData
import io.scalacraft.core.clientbound.PlayPackets.WindowItems
import io.scalacraft.core.clientbound.PlayPackets
import io.scalacraft.core.serverbound.PlayPackets.{AddPlayer, PlayerInfo}
import io.scalacraft.core.{Helpers, Marshallers, PacketManager}

import scala.language.postfixOps

object Entrypoint extends App {
  val pm = new PacketManager[PlayPackets.type]

  //val originalPacket = Helpers.hex2bytes("0001e5620206432e3626b7f44ee340bcfd0a0965636961766174746100000000")
  val test = WindowItems(5,2, Some(SlotData(1,2,3)))
  val serializedPacket = new ByteArrayOutputStream()
  //implicit val inStream: BufferedInputStream = new BufferedInputStream(new ByteArrayInputStream(originalPacket))
  implicit val outStream: BufferedOutputStream = new BufferedOutputStream(serializedPacket)

  pm.marshal(test)

  //inStream.close()
  outStream.close()

  println(serializedPacket.toByteArray)
  //assert(originalPacket.toList sameElements serializedPacket.toByteArray)

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
