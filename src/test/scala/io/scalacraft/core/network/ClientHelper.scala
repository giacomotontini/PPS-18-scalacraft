package io.scalacraft.core.network

import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel.{ChannelHandlerContext, ChannelInboundHandlerAdapter, ChannelInitializer}
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.scalacraft.misc.{Helpers, ServerConfiguration}
import org.scalatest.Matchers

case class ClientHelper(toSend: Option[String], expected: Option[String]) extends Matchers{

  private[this] class ClientHandler() extends ChannelInboundHandlerAdapter {

    override def channelActive(ctx: ChannelHandlerContext): Unit = {
      toSend match {
        case Some(message) =>
          ctx.writeAndFlush(ctx.alloc().buffer().writeBytes(Helpers.hex2bytes(message)));
      }
    }

    override def channelRead(channelHandlerContext: ChannelHandlerContext, message: Object): Unit = {
      expected match {
        case Some(messageExpected) =>
          val messageByteBuf = message.asInstanceOf[ByteBuf]
          try {
            val byteOfExpectedString = messageExpected.getBytes
            if(messageByteBuf.readableBytes() >= byteOfExpectedString.length) {
                toSend.get shouldBe Helpers.bytes2hex(messageByteBuf.readBytes(byteOfExpectedString.length).array())
            }
            channelHandlerContext.close()
          } finally {
            messageByteBuf.release()
          }
      }
    }

    override def exceptionCaught(channelHandlerContext: ChannelHandlerContext, cause: Throwable): Unit = {
      channelHandlerContext.close()
    }
  }

  def run(): Unit = {
    val host = "localhost"
    val workerGroup = new NioEventLoopGroup()
    try {
      val bootstrap = new Bootstrap()
      bootstrap.group(workerGroup)
        .channel(classOf[NioSocketChannel])
        .handler(new ChannelInitializer[SocketChannel] {
          override def initChannel(channel: SocketChannel): Unit = {
            channel.pipeline().addLast(new ClientHandler())
          }
        })
      bootstrap.connect(host, ServerConfiguration.PORT).sync()
    }
  }
}
