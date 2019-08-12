package io.scalacraft

import akka.actor.ActorSystem
import com.typesafe.scalalogging.LazyLogging
import io.scalacraft.core.network.{Server, ServerHandler}
import io.scalacraft.logic.World
import io.scalacraft.misc.ServerConfiguration

import scala.language.postfixOps

object Entrypoint extends App with LazyLogging {

  logger.debug("Starting main ActorSystem..")
  val system = ActorSystem("scalacraft")
  val world = system.actorOf(World.props, World.name)

  val server = Server(ServerConfiguration.Port, () => new ServerHandler(system))
  server.run()

}
