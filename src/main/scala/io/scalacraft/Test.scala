package io.scalacraft

import scala.io.Source
import io.circe.parser._
import io.circe.generic.auto._
import io.circe.parser
import io.scalacraft.logic.traits.creatures.SpawnCreatureResult
import io.scalacraft.packets.DataTypes.Position
import net.querz.nbt.mca.MCAUtil

import scala.util.Random


case object Test extends App {

  /*case class State(properties: Option[Map[String, String]], id: Int, default: Option[Boolean])
  case class Block(properties: Option[Map[String, List[String]]], states: List[State])

  val input_file = "src/main/resources/data/blocks.json"
  val json_content = Source.fromFile(input_file).mkString
  val json_data = parse(json_content)

  val decodeResult = parser.decode[Map[String, Block]](json_content)

  decodeResult.right.get.foreach(kv => {
    println(kv._1, kv._2)
  })*/
  protected def spawn(positionsTest: Map[Int, Set[(Position, Boolean)]] ): Map[Int, Set[(Position, Boolean)]] = {
    var unusedPositions: Map[Int, Set[(Position, Boolean)]] = Map()
    var positions = positionsTest(1).collect {
      case (position, isWater) if !isWater => position
    }
    for (i <- 0 to Random.nextInt(positionsTest.size)) {
      val biome = 1
      val position = positions.toVector(Random.nextInt(positions.size))
      positions -= position
      println(position)
      unusedPositions = Map(biome -> positions.map(position => (position, false)))
      }
    unusedPositions
    }

  println(spawn(Map(1 -> Set((Position(0,0,0),false), (Position(1,1,1), false), (Position(1,2,3),false)))))

}
