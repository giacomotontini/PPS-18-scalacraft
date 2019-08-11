package io.scalacraft.misc

import java.nio.charset.Charset
import java.util.Base64

import io.scalacraft.packets.clientbound.PlayPackets.{GameModeValue, LevelType, ServerDifficulties}

import scala.io.Source

object ServerConfiguration {

  private[this] val image = Source.fromInputStream(getClass.getResourceAsStream("/server-logo.png"), "iso-8859-1").mkString
  private[this] val favicon = Base64.getEncoder.encodeToString(image.getBytes(Charset.forName("iso-8859-1")))

  val Debug: Boolean = true
  val Port: Int = 25565
  val VersionName: String = "1.13.2"
  val VersionProtocol: Int = 404
  val Online: Int = 0
  val GameMode: GameModeValue.Survival.type = GameModeValue.Survival
  val ServerDifficulty: ServerDifficulties.Easy.type = ServerDifficulties.Easy
  val ReducedDebugInfo: Boolean = false
  val MaxPlayers: Int = 100
  val LevelTypeBiome: LevelType = LevelType.Default

  def configuration: String =
    s"""{"version": {"name": "$VersionName", "protocol": $VersionProtocol},"players": {"max": $MaxPlayers,"online": $Online},"description": {"text": "Scalacraft Server"},"favicon": "data:image/png;base64,$favicon"}"""
    //{"description":{"text":"A Minecraft Server"},"players":{"max":20,"online":0},"version":{"name":"1.13.2","protocol":404}}
}
