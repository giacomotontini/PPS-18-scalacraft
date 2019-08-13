package io.scalacraft.core.network

import com.typesafe.scalalogging.LazyLogging
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.{Channel, ChannelInboundHandlerAdapter, ChannelInitializer}

import scala.language.postfixOps

class Server(port: Int, handler: () => ChannelInboundHandlerAdapter) extends LazyLogging {

  val bossGroup = new NioEventLoopGroup()
  val workerGroup = new NioEventLoopGroup()
  var socketChannel: Channel = _

  def run(): Unit = {
    logger.info(s"Starting server at port $port")

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

  def apply(port: Int, serverHandler: () => ServerHandler): Server = new Server(port, serverHandler)

}
