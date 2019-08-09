package io.scalacraft.misc

import scala.io.Source
import io.circe.generic.auto._
import io.circe.parser
import net.querz.nbt.CompoundTag

object Blocks {

  private case class State(properties: Option[Map[String, String]], id: Int, default: Option[Boolean])

  private case class Block(states: List[State], properties: Option[Map[String, List[String]]])

  //The index represents the block id, the compound tag represents the block state
  private lazy val compoundTagList = {
    val inputFile = "/blocks.json"
    val content = Source.fromInputStream(getClass.getResourceAsStream(inputFile)).mkString
    val parsed = parser.decode[Map[String, Block]](content)
    var resultMap: Map[String, Block] = Map()
    parsed match {
      case Right(result) => resultMap = result
    }
    val idToCompoundTag = resultMap.flatMap {
      case (blockName, blockObject) =>
        blockObject.states.map(blockState => {
          val compoundTag = new CompoundTag()
          compoundTag.putString("Name", blockName)
          blockState.properties.map(property => {
            val innerTag = new CompoundTag()
            property.foreach {
              case (name, value) =>
                innerTag.putString(name, value)
            }
            innerTag
          }).foreach(tag => compoundTag.put("Properties", tag))
          blockState.id -> compoundTag
        })
    }
    idToCompoundTag.toList.sortBy(_._1).map(_._2)
  }

  def compoundTagFromId(id: Int): CompoundTag = {
    compoundTagList(id)
  }

  def idFromCompoundTag(compoundTag: CompoundTag): Int = {
    compoundTagList.indexOf(compoundTag)
  }
}
