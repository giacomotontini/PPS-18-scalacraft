package io.scalacraft

import akka.actor.ActorSystem
import com.typesafe.scalalogging.LazyLogging
import io.scalacraft.core.network.{Server, ServerHandler}
import io.scalacraft.logic.World
import io.scalacraft.misc.ServerConfiguration
import io.scalacraft.packets.clientbound.PlayPackets.{GameModeValue, LevelType, ServerDifficulties}
import scopt.OParser

import scala.language.postfixOps

object Entrypoint extends App with LazyLogging {

  val builder = OParser.builder[ServerConfiguration]
  val parser = {
    import builder._

    OParser.sequence(
      programName(ServerConfiguration.Name),
      head(ServerConfiguration.Name, ServerConfiguration.Version),
      opt[Unit]("debug")
        .action((_, c) => c.copy(debug = true))
        .text("enable debug mode"),
      opt[Unit]("reduced-debug-info")
        .action((_, c) => c.copy(reducedDebugInfo = true))
        .text("disable in-game debug info"),
      opt[Int]('p', "port")
        .action((p, c) => c.copy(port = p))
        .text("the port listen on"),
      opt[String]("description")
        .action((d, c) => c.copy(serverDescription = d))
        .text("the server description"),
      opt[String]("game-mode")
        .validate(g => if (parseGameMode.isDefinedAt(g)) success else failure("invalid game mode"))
        .action((g, c) => c.copy(gameMode = parseGameMode(g))),
      opt[String]("server-difficulty")
        .validate(d => if (parseServerDifficulty.isDefinedAt(d)) success else failure("invalid server difficulty"))
        .action((d, c) => c.copy(serverDifficulty = parseServerDifficulty(d))),
      opt[Int]("max-players")
        .action((m, c) => c.copy(maxPlayers = m))
        .text("the maximum number of players"),
      opt[String]("biome-type")
        .validate(b => if (parseLevelTypeBiome.isDefinedAt(b)) success else failure("invalid biome type"))
        .action((b, c) => c.copy(levelTypeBiome = parseLevelTypeBiome(b)))
    )
  }

  OParser.parse(parser, args, ServerConfiguration()) match {
    case Some(config) => startServer(config)
    case _ =>  // arguments are bad, error message will have been displayed
  }

  private def parseGameMode: PartialFunction[String, GameModeValue] = {
    case "adventure" => GameModeValue.Adventure
    case "creative" => GameModeValue.Creative
    case "spectator" => GameModeValue.Spectator
    case "survival" => GameModeValue.Survival
  }

  private def parseServerDifficulty: PartialFunction[String, ServerDifficulties] = {
    case "normal" => ServerDifficulties.Normal
    case "hard" => ServerDifficulties.Hard
    case "easy" => ServerDifficulties.Easy
    case "peaceful" => ServerDifficulties.Peaceful
  }

  private def parseLevelTypeBiome: PartialFunction[String, LevelType] = {
    case "default" => LevelType.Default
    case "amplified" => LevelType.Amplified
    case "buffet" => LevelType.Buffet
    case "custom" => LevelType.Custom
    case "flat" => LevelType.Flat
    case "largebiomes" => LevelType.LargeBiomes
  }

  private def startServer(config: ServerConfiguration): Unit = {
    logger.debug("Starting main ActorSystem..")
    val system = ActorSystem("scalacraft")
    val world = system.actorOf(World.props(config), World.name)

    val server = Server(config.port, () => new ServerHandler(system, config))
    server.run()
  }

}
