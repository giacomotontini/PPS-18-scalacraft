package io.scalacraft.core.network

import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel.{ChannelHandlerContext, ChannelInboundHandlerAdapter, ChannelInitializer}
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.scalacraft.misc.{Helpers, ServerConfiguration}
import org.scalatest.Matchers

class ClientHelper(result: String=>Unit) extends Matchers {

  private var context: ChannelHandlerContext = _

  class ClientHandler() extends ChannelInboundHandlerAdapter {

    override def channelActive(ctx: ChannelHandlerContext): Unit = context = ctx

    override def channelRead(channelHandlerContext: ChannelHandlerContext, message: Object): Unit = {
      val buffer = message.asInstanceOf[ByteBuf]
      val array = new Array[Byte](buffer.readableBytes())
      buffer.readBytes(array)

      result(Helpers.bytes2hex(array))
    }

  }

  def writeHex(payload: String): Unit = {
    context.writeAndFlush(context.alloc().buffer().writeBytes(Helpers.hex2bytes(payload)))
  }

  def run(): Unit = {
    val workerGroup = new NioEventLoopGroup()

    val bootstrap = new Bootstrap()
    bootstrap.group(workerGroup)
      .channel(classOf[NioSocketChannel])
      .handler(new ChannelInitializer[SocketChannel] {
        override def initChannel(channel: SocketChannel): Unit = {
          channel.pipeline().addLast(new ClientHandler())
        }
      })
    bootstrap.connect("localhost", ServerConfiguration.PORT).sync()
  }

}
