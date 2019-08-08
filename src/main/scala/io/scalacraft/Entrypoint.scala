package io.scalacraft

import akka.actor.ActorSystem
import io.scalacraft.core.network.Server
import io.scalacraft.logic.World
import io.scalacraft.misc.ServerConfiguration

import scala.language.postfixOps

object Entrypoint extends App {

  val system = ActorSystem("scalacraft")
  val world = system.actorOf(World.props)

  val server = Server.withDefaultServerHandler(ServerConfiguration.Port)
  server.run()


}
