package io.scalacraft

import scala.io.Source
import io.circe.parser._
import io.circe.generic.auto._
import io.circe.parser
import io.circe.syntax._


case object Test extends App {

  case class State(properties: Option[Map[String, String]], id: Int, default: Option[Boolean])
  case class Block(properties: Option[Map[String, List[String]]], states: List[State])

  val input_file = "src/main/resources/blocks.json"
  val json_content = Source.fromFile(input_file).mkString
  val json_data = parse(json_content)

  val decodeResult = parser.decode[Map[String, Block]](json_content)

  decodeResult.right.get.foreach(kv => {
    println(kv._1, kv._2)
  })
}
