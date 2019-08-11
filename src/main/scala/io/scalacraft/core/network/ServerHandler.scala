package io.scalacraft.core.network

import akka.actor.{ActorRef, ActorSystem}
import com.typesafe.scalalogging.LazyLogging
import io.netty.channel.{ChannelHandlerContext, ChannelInboundHandlerAdapter}
import io.scalacraft.logic.UserContext

class ServerHandler(actorSystem: ActorSystem) extends ChannelInboundHandlerAdapter with LazyLogging {

  var dispatcher: ActorRef = _

  override def handlerAdded(ctx: ChannelHandlerContext): Unit =
    dispatcher = actorSystem.actorOf(UserContext.props(ConnectionManager(ctx)), UserContext.name)

  override def channelRead(channelHandlerContext: ChannelHandlerContext, message: Object): Unit =
    dispatcher ! message.asInstanceOf[RawPacket]

  override def exceptionCaught(channelHandlerContext: ChannelHandlerContext, cause: Throwable): Unit = {
    logger.error(cause.getMessage)
    channelHandlerContext.close()
  }

}
