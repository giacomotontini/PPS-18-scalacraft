package io.scalacraft.misc

import java.nio.charset.Charset
import java.util.Base64

import scala.io.Source

object ServerConfiguration {

  val PORT = 25565
  private[this] val online = 5
  private[this] val image = Source.fromInputStream(getClass.getResourceAsStream("/server-logo.png"), "iso-8859-1").mkString
  private[this] val favicon = Base64.getEncoder.encodeToString(image.getBytes(Charset.forName("iso-8859-1")))

  def configuration: String =
    s"""{
    "version": {
      "name": "1.8.7",
      "protocol": 47
    },
    "players": {
      "max": 100,
      "online": $online,
      "sample": [
    {
      "name": "thinkofdeath",
      "id": "4566e69f-c907-48ee-8d71-d7ba5aa00d20"
    }
      ]
    },
    "description": {
      "text": "Hello world"
    },
    "favicon": "data:image/png;base64$favicon"
  }"""
}
