package io.scalacraft.core.network

import io.netty.channel.{ChannelHandlerContext, ChannelInboundHandlerAdapter}
import io.scalacraft.misc.Helpers
import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers}


class ServerSpec extends FlatSpec with Matchers with BeforeAndAfterEach {

  var server: Server = _
  var client: ClientHelper = _
  var context: ChannelHandlerContext = _
  var rawPacket: RawPacket = _
  var result: String = ""

  private def checkResult(packetId: Int, payload: String): Unit = {
    Thread.sleep(500) // wait the message arrives
    val buffer = new Array[Byte](payload.length / 2)
    rawPacket.payload.readFully(buffer)
    rawPacket.packetId shouldBe packetId
    Helpers.bytes2hex(buffer) shouldBe payload
  }

  override def beforeEach() {
    server = new Server(ServerSpec.testPort, () => new ChannelInboundHandlerAdapter {
      override def handlerAdded(ctx: ChannelHandlerContext): Unit = context = ctx

      override def channelRead(channelHandlerContext: ChannelHandlerContext, message: Object): Unit = {
        rawPacket = message.asInstanceOf[RawPacket]
      }
    })
    server.run()

    client = new ClientHelper(str => result = str)
    client.run()

    Thread.sleep(500)
  }

  override def afterEach() {
    server.stop()
  }

  behavior of "A Server"

  it should "receive the correct message" in {
    client.writeHex("03010102")
    checkResult(1, "0102")
  }

  it should "receive the correct empty message" in {
    client.writeHex("0101")
    checkResult(1, "")
  }

  it should "send a packet to a client" in {
    client.writeHex("0101")

    val buffer = context.alloc().buffer()
    buffer.writeBytes(Helpers.hex2bytes("03010102"))
    context.writeAndFlush(buffer)

    Thread.sleep(500)
    result shouldBe "03010102"
  }

}

object ServerSpec {

  val testPort: Int = 26666

}
