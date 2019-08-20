package io.scalacraft.loaders

import io.circe.generic.auto._
import io.circe.parser
import net.querz.nbt.CompoundTag

import scala.io.Source
import scala.language.postfixOps

object Blocks extends App {

  case class State(default: Boolean, id: Int, properties: Map[String, String])

  case class BreakingProperties(canBeHarvested: Boolean, dropSomething: Boolean, value: Float)

  case class Drop(minDrop: Option[Float], maxDrop: Option[Float], namespace: String, `type`: String)

  case class Block(blastResistance: Option[Float],
                   blockId: Int,
                   breaking: Map[String, BreakingProperties],
                   drops: List[Drop],
                   gravity: Boolean,
                   hardness: Option[Float],
                   light: Int,
                   name: String,
                   namespace: String,
                   renewable: Boolean,
                   stackable: Int,
                   states: List[State],
                   tool: Option[String],
                   tool2: Option[String],
                   transparent: Boolean,
                   `type`: Option[String])


  private lazy val blocks = {
    val content = Source.fromInputStream(getClass.getResourceAsStream("/data/blocks.json")).mkString
    val Right(blocks) = parser.decode[List[Block]](content)
    blocks
  }

  private lazy val compoundTagList = {
    blocks.flatMap { block =>
      block.states map { state =>
        val compoundTag = new CompoundTag()
        compoundTag.putString("Name", block.namespace)

        if (state.properties.nonEmpty) {
          val innerTag = new CompoundTag
          state.properties foreach {
            case (name, value) => innerTag.putString(name, value)
          }
          compoundTag.put("Properties", innerTag)
        }

        state.id -> compoundTag
      }
    } sortBy { case (id, _) => id } map { case (_, tag) => tag }
  }

  def compoundTagFromBlockName(name: String): CompoundTag = {
    compoundTagList find { _.getString("Name") == s"minecraft:$name" } get
  }

  def compoundTagFromBlockStateId(blockStateId: Int): CompoundTag = {
    compoundTagList(blockStateId)
  }

  def compoundTagFromBlockId(blockId: Int): CompoundTag = {
    val defaultStateId = (blocks find { _.blockId == blockId } flatMap { _.states.find(_.default) } map { _.id }).get
    compoundTagList(defaultStateId)
  }

  def blockFromCompoundTag(compoundTag: CompoundTag): Block = blockFromStateId(stateIdFromCompoundTag(compoundTag))

  def stateIdFromCompoundTag(compoundTag: CompoundTag): Int = {
    val stateId = compoundTagList.indexOf(compoundTag)
    assert(stateId >= 0)
    stateId
  }

  def blockFromStateId(stateId: Int): Block = {
    blocks find { _.states.exists(_.id == stateId) } get
  }

  def blockFromBlockId(blockId: Int): Block = {
    blocks(blockId)
  }

}