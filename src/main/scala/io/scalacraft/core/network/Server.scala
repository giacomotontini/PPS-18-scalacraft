package io.scalacraft.core.network

import java.io.{ByteArrayInputStream, DataInputStream}
import java.util

import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.{Channel, ChannelHandlerContext, ChannelInboundHandlerAdapter, ChannelInitializer}
import io.netty.handler.codec.ByteToMessageDecoder
import io.scalacraft.misc.ServerConfiguration

import scala.language.postfixOps

class Server(port: Int, handler:() => ChannelInboundHandlerAdapter) {

  val bossGroup = new NioEventLoopGroup()
  val workerGroup = new NioEventLoopGroup()
  var socketChannel: Channel = _

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
          terminated = (read & 0x80) == 0
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
      if (packetId == -1 && packetLength != -1) {
        if (readVarInt()) {
          packetId = result
          result = 0
        }
      }
      if (in.readableBytes() >= packetLength - numRead && packetId != -1 && packetLength != -1) {
        val buff: Array[Byte] = new Array[Byte](packetLength - numRead)
        in.readBytes(buff)
        out.add(RawPacket(packetId, new DataInputStream(new ByteArrayInputStream(buff))))
        packetLength = -1
        packetId = -1
        numRead = 0
      }
    }
  }

  def run(): Unit = {
    val serverBootstrap = new ServerBootstrap()
    serverBootstrap.group(bossGroup, workerGroup)
      .channel(classOf[NioServerSocketChannel])
      .childHandler(new ChannelInitializer[SocketChannel] {
        override def initChannel(channel: SocketChannel): Unit = {
          channel.pipeline().addLast(new MessageDecoder(), handler())
        }
      })

    val bindFuture = serverBootstrap.bind(port).sync()
    socketChannel = bindFuture.channel()
  }

  def stop(): Unit = {
    socketChannel.close().sync()
    workerGroup.shutdownGracefully()
    bossGroup.shutdownGracefully()
  }

}

object Server extends App {
  def withDefaultServerHandler(port: Int): Server = new Server(ServerConfiguration.Port, () => new ServerHandler())

  withDefaultServerHandler(ServerConfiguration.Port).run()
}
