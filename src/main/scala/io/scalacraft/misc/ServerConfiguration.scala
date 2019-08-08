package io.scalacraft.misc

import java.nio.charset.Charset
import java.util.Base64

import io.scalacraft.packets.clientbound.PlayPackets.{GameModeValue, LevelType, ServerDifficulties}

import scala.io.Source

object ServerConfiguration {

  private[this] val image = Source.fromInputStream(getClass.getResourceAsStream("/server-logo.png"), "iso-8859-1").mkString
  private[this] val favicon = Base64.getEncoder.encodeToString(image.getBytes(Charset.forName("iso-8859-1")))

  val Port = 25565
  val VersionName = "1.13.2"
  val VersionProtocol = 404
  val Online = 0
  val GameMode = GameModeValue.Survival
  val ServerDifficulty = ServerDifficulties.Easy
  val ReducedDebugInfo = false
  val MaxPlayers = 100
  val LevelTypeBiome = LevelType.Default

  def configuration: String =
    s"""{"version": {"name": "$VersionName", "protocol": $VersionProtocol},"players": {"max": $MaxPlayers,"online": $Online},"description": {"text": "Scalacraft Server"},"favicon": "data:image/png;base64,$favicon"}"""
    //{"description":{"text":"A Minecraft Server"},"players":{"max":20,"online":0},"version":{"name":"1.13.2","protocol":404}}
}
