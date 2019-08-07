package io.scalacraft.core.network

import io.netty.channel.{ChannelHandlerContext, ChannelInboundHandlerAdapter}
import io.scalacraft.misc.Helpers
import org.scalatest.Matchers

class DummyServerHandler(packetIdExpected: Int, payloadStringExpected: String, payloadLength: Int) extends ChannelInboundHandlerAdapter with Matchers{

  override def channelRead(channelHandlerContext: ChannelHandlerContext, message: Object): Unit = {
    val rawPacket = message.asInstanceOf[RawPacket]
    val payloadArray = new Array[Byte](payloadLength)
    rawPacket.payload.readFully(payloadArray)
    rawPacket.packetId shouldBe packetIdExpected
    Helpers.bytes2hex(payloadArray) shouldBe payloadStringExpected
    println(rawPacket.packetId, Helpers.bytes2hex(payloadArray))
  }

  override def exceptionCaught(channelHandlerContext: ChannelHandlerContext, cause: Throwable): Unit = {
    channelHandlerContext close
  }
}
