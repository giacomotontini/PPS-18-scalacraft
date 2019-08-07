package io.scalacraft.core.network

import java.io.{ByteArrayInputStream, DataInputStream}
import java.util

import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.{ChannelHandlerContext, ChannelInboundHandlerAdapter, ChannelInitializer}
import io.netty.handler.codec.ByteToMessageDecoder
import io.scalacraft.core.fsm.ConnectionController
import io.scalacraft.misc.ServerConfiguration

import scala.language.postfixOps

private[this] class Server(val port: Int) {

  case class RawPacket(packetId: Int, payload: DataInputStream)

  private[this] class MessageDecoder() extends ByteToMessageDecoder {
    var packetLength, packetId = -1
    var numRead, result = 0

    override def decode(ctx: ChannelHandlerContext, in: ByteBuf, out: util.List[AnyRef]): Unit = {
      def readVarInt(): Boolean = {
        var terminated = false
        do {
          val read = in.readByte()
          result |= ((read & 0x7f) << (7 * numRead))
          numRead += 1
          if (numRead > 5) {
            throw new IllegalArgumentException("VarInt is too big")
          }
          terminated = (read & 0x80) != 0
        } while (!terminated && in.readableBytes() > 0)
        terminated
      }

      if (packetLength == -1) {
        if (readVarInt()) {
          packetLength = result
          result = 0
          numRead = 0
        }
      }
      if (packetId == -1) {
        if (readVarInt()) {
          packetId = result
          result = 0
        }
      }
      val length =  packetLength - numRead
      if (in.readableBytes() >= length) {
        val buff: Array[Byte] = new Array[Byte](length)
        in.readBytes(buff)
        out.add(RawPacket(packetId, new DataInputStream(new ByteArrayInputStream(buff))))
        packetLength = -1
        packetId = -1
        numRead = 0
      }
    }
  }

  private[this] class ServerHandler() extends ChannelInboundHandlerAdapter {
    var connectionController: ConnectionController = _

    override def handlerAdded(ctx: ChannelHandlerContext): Unit = {
      val connectionManager: ConnectionManager = ConnectionManager(ctx)
      connectionController = new ConnectionController(connectionManager)
    }
    override def channelRead(channelHandlerContext: ChannelHandlerContext, message: Object): Unit = {
      val rawPacket = message.asInstanceOf[RawPacket]
      connectionController.handlePacket(rawPacket.packetId, rawPacket.payload)
    }

    override def exceptionCaught(channelHandlerContext: ChannelHandlerContext, cause: Throwable): Unit = {
      //logger.info(cause.getMessage)
      channelHandlerContext close
    }
  }


  def run(): Unit = {
    val bossGroup = new NioEventLoopGroup();
    val workerGroup = new NioEventLoopGroup();
    try {
      val serverBootstrap = new ServerBootstrap();
      serverBootstrap.group(bossGroup, workerGroup)
        .channel(classOf[NioServerSocketChannel])
        .childHandler(new ChannelInitializer[SocketChannel] {
          override def initChannel(channel: SocketChannel): Unit = channel.pipeline().addLast(new MessageDecoder(), new ServerHandler())
        })
      val channelClosedFuture = serverBootstrap.bind(port).sync();
      channelClosedFuture.channel().closeFuture().sync();
    } finally {
      workerGroup.shutdownGracefully();
      bossGroup.shutdownGracefully();
    }
  }
}

object Server extends App {
  new Server(ServerConfiguration.PORT).run()
}
