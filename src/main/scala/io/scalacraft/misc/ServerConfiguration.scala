package io.scalacraft.misc

import java.nio.charset.Charset
import java.util.Base64

import io.scalacraft.packets.clientbound.PlayPackets.{GameModeValue, LevelType, ServerDifficulties}

import scala.io.Source

case class ServerConfiguration(debug: Boolean = false,
                               port: Int = 25565,
                               serverDescription: String = "Scalacraft Server",
                               gameMode: GameModeValue = GameModeValue.Survival,
                               serverDifficulty: ServerDifficulties = ServerDifficulties.Easy,
                               maxPlayers: Int = 100,
                               levelTypeBiome: LevelType = LevelType.Default) {

  import ServerConfiguration._

  def loadConfiguration(onlinePlayers: Int): String =
    s"""{"version": {"name": "$VersionName", "protocol": $VersionProtocol},"players": {"max": $maxPlayers,"online": $onlinePlayers},"description": {"text": "$serverDescription"},"favicon": "data:image/png;base64,$favicon"}"""

}

object ServerConfiguration {

  private val image = Source.fromInputStream(getClass.getResourceAsStream("/server-logo.png"), "iso-8859-1").mkString
  private val favicon = Base64.getEncoder.encodeToString(image.getBytes(Charset.forName("iso-8859-1")))

  val Name: String = "scalacraft"
  val Version: String = "0.1"
  val VersionName: String = "1.13.2"
  val VersionProtocol: Int = 404
  val TicksInSecond: Int = 20

}
