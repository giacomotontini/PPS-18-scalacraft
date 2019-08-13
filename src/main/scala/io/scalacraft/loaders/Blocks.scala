package io.scalacraft.loaders

import io.circe.generic.auto._
import io.circe.parser
import io.scalacraft.misc.Helpers
import net.querz.nbt.CompoundTag

import scala.io.Source

object Blocks {

  case class State(name: String,
                   `type`: String,
                   values: Option[List[String]],
                   num_values: Int)

  case class Block(id: Int,
                   displayName: String,
                   name: String,
                   hardness: Double,
                   minStateId: Int,
                   maxStateId: Int,
                   states: List[State],
                   drops: List[Int],
                   diggable: Boolean,
                   transparent: Boolean,
                   filterLight: Int,
                   emitLight: Int,
                   boundingBox: String,
                   stackSize: Int,
                   material: Option[String],
                   harvestTools: Option[Map[String, Boolean]])

  private lazy val blocks = {
    val content = Source.fromInputStream(getClass.getResourceAsStream("/data/blocks.json")).mkString
    val Right(blocks) = parser.decode[List[Block]](content)
    blocks
  }

   private lazy val compoundTagList = {
    blocks.flatMap {
      block: Block =>
        val allStatesLists = block.states.map(state => state.`type` match {
          case "bool" => List(true, false)
          case "int" => List.range(1, state.num_values + 1)
          case _ => state.values.get
        })
        val properties = Helpers.cartesianProduct(allStatesLists: _*)

        for (stateId <- block.minStateId to block.maxStateId) yield {
          val actualState = stateId - block.minStateId
          val compoundTag = new CompoundTag()
          val innerTag = new CompoundTag
          compoundTag.putString("name", block.name)
          if (properties.nonEmpty)
            properties(actualState).zipWithIndex.foreach {
              case (value, index) => innerTag.putString(block.states(index).name, value.toString)
            }
          compoundTag.put("properties", innerTag)
          stateId -> compoundTag
        }
    }.sortBy(_._1) map(_._2)
  }


  def compoundTagFromBlockStateId(blockStateId: Int): CompoundTag = {
    compoundTagList(blockStateId)
  }


  def stateIdFromCompoundTag(compoundTag: CompoundTag): Int = {
    compoundTagList.indexOf(compoundTag)
  }


  def blockFromStateId(stateId: Int): Block = {
      blocks.filter(b => b.minStateId >= stateId && b.maxStateId <= stateId).head
  }

  def blockFromBlockId(blockId: Int): Block = {
    blocks(blockId)
  }

}
