package io.scalacraft.core.network

import akka.actor.{ActorRef, ActorSystem}
import com.typesafe.scalalogging.LazyLogging
import io.netty.channel.{ChannelHandlerContext, ChannelInboundHandlerAdapter}
import io.scalacraft.logic.UserContext
import io.scalacraft.logic.messages.Message.UserDisconnected
import io.scalacraft.misc.ServerConfiguration

class ServerHandler(actorSystem: ActorSystem, serverConfiguration: ServerConfiguration)
  extends ChannelInboundHandlerAdapter with LazyLogging {

  var dispatcher: ActorRef = _

  override def handlerAdded(ctx: ChannelHandlerContext): Unit = {
    logger.debug(s"Accepting new connection from address ${ctx.channel().remoteAddress()}")
    dispatcher = actorSystem.actorOf(UserContext.props(ConnectionManager(ctx), serverConfiguration), UserContext.name)
  }

  override def channelRead(channelHandlerContext: ChannelHandlerContext, message: Object): Unit =
    dispatcher ! message.asInstanceOf[RawPacket]

  override def exceptionCaught(channelHandlerContext: ChannelHandlerContext, cause: Throwable): Unit = {
    logger.error(cause.getMessage)
    channelHandlerContext.close()
  }

  override def channelInactive(ctx: ChannelHandlerContext): Unit = {
    logger.debug(s"Closing connection of address ${ctx.channel().remoteAddress()}")
    dispatcher ! UserDisconnected
  }

}
