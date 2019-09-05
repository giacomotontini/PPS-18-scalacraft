package io.scalacraft.misc

import java.nio.charset.Charset
import java.util.Base64

import io.scalacraft.core.packets.clientbound.PlayPackets.{GameModeValue, LevelType, ServerDifficulties}

import scala.io.Source

/**
 * Contains the dynamic server properties.
 */
case class ServerConfiguration(debug: Boolean = false,
                               port: Int = 25565,
                               serverDescription: String = "Scalacraft Server",
                               gameMode: GameModeValue = GameModeValue.Survival,
                               serverDifficulty: ServerDifficulties = ServerDifficulties.Easy,
                               maxPlayers: Int = 20,
                               levelTypeBiome: LevelType = LevelType.Default,
                               reducedDebugInfo: Boolean = false) {

  import ServerConfiguration._

  /**
   * Generate the configuration string that must be send to client.
   *
   * @param onlinePlayers the number of current online players
   * @return the configuration in json format
   */
  def loadConfiguration(onlinePlayers: Int): String =
    s"""{"version": {"name": "$VersionName", "protocol": $VersionProtocol},"players": {"max": $maxPlayers,"online": $onlinePlayers},"description": {"text": "$serverDescription"},"favicon": "data:image/png;base64,$favicon"}"""

}

/**
 * Contains the static server properties.
 */
object ServerConfiguration {

  private val image = Source.fromInputStream(getClass.getResourceAsStream("/server-logo.png"), "iso-8859-1").mkString
  private val favicon = Base64.getEncoder.encodeToString(image.getBytes(Charset.forName("iso-8859-1")))

  val Name: String = "scalacraft"
  val Version: String = "1.0"
  val VersionName: String = "1.13.2"
  val VersionProtocol: Int = 404
  val TicksInSecond: Int = 20
  val MaxViewDistance: Int = 10
  val LoadingChunksBlocksThreshold = 8 // not compute needed chunks if player moves under this

}
