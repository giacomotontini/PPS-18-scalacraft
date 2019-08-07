package io.scalacraft.core.network

import io.netty.channel.{ChannelHandlerContext, ChannelInboundHandlerAdapter}
import io.scalacraft.core.fsm.ConnectionController

class ServerHandler() extends ChannelInboundHandlerAdapter {
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
