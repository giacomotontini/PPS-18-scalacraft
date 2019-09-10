package io.scalacraft.loaders

import io.circe.generic.auto._
import io.circe.parser
import io.scalacraft.core.packets.DataTypes.{BlockStateId, Identifier}
import net.querz.nbt.CompoundTag

import scala.io.Source

/**
 * A data loader for blocks
 */
object Blocks {

  /**
   * Represent a possible block's state
   * @param default is the default state?
   * @param id the state id
   * @param properties the properties of this state
   */
  case class State(default: Boolean, id: BlockStateId, properties: Map[String, String])

  /**
   * Represent the block breaking properties.
   * @param canBeHarvested if the block could be broken with a specific tool made of specific material
   * @param dropSomething true if the block drops something if broken with a specific tool made of specific material
   * @param value the time needed to break this block with a specific tool made of specific material
   */
  case class BreakingProperties(canBeHarvested: Boolean, dropSomething: Boolean, value: Float)

  /**
   * Represent a block's drop
   * @param minDrop the minimum number of items to be dropped
   * @param maxDrop the maximum number of items to be dropped
   * @param namespace the name of the dropped item
   * @param `type` the type of the dropped item
   */
  case class Drop(minDrop: Option[Float], maxDrop: Option[Float], namespace: String, `type`: String)

  /**
   * Represent a minecraft block.
   * @param blastResistance the resistance of the block subject to blast
   * @param blockId the id of the block
   * @param breaking map a material to its specific breaking properties
   * @param drops a list of items to be dropped
   * @param gravity has gravity
   * @param hardness the block hardness
   * @param light the light which this block emit
   * @param name the name of the block
   * @param namespace the namespace of the block. Usually "minecraft:{name}"
   * @param renewable true if the block could be renewed
   * @param stackable the maximum number of item that could stay on the same inventory slot
   * @param states the list of all the possible state of a block
   * @param tool the main tool that could break this block if any
   * @param tool2 the second tools that could break this block if any
   * @param transparent true if it's transparent
   * @param `type` the type of the block
   */
  case class Block(blastResistance: Option[Float],
                   blockId: Int,
                   breaking: Map[String, BreakingProperties],
                   drops: List[Drop],
                   gravity: Boolean,
                   hardness: Option[Float],
                   light: Byte,
                   name: String,
                   namespace: String,
                   renewable: Boolean,
                   stackable: Int,
                   states: List[State],
                   tool: Option[String],
                   tool2: Option[String],
                   transparent: Boolean,
                   `type`: Option[String])

  private lazy val fluids = List("minecraft:lava", "minecraft:water")

  private lazy val blocks = {
    val content = Source.fromInputStream(getClass.getResourceAsStream("/data/blocks.json")).mkString
    val Right(blocks) = parser.decode[List[Block]](content)
    blocks
  }

  /*
   * Map all blocks states to a list of compound tag
   */
  private lazy val compoundTagList = blocks.flatMap { block =>
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

  private lazy val blocksTags = blocks map { block =>
    block.namespace -> block.states.map(_.id)
  } toMap

  /**
   * Retrieve a compound tag relative to a default block's state
   * @param name the name of the block
   * @return the default compound tag for a specific block's name
   */
  def defaultCompoundTagFromName(name: String): Option[CompoundTag] =
    blocks find { _.name == name } flatMap { _.states.find(_.default) } map { s => compoundTagFromBlockStateId(s.id) }

  /**
   * Retrieve a compound tag relative to the specified block state id
   * @param blockStateId the block's state id
   * @return a compound tag for a specific block state id
   */
  def compoundTagFromBlockStateId(blockStateId: BlockStateId): CompoundTag = compoundTagList(blockStateId)

  /**
   * Retrieve a compound tag relative to the specified block id
   * @param blockId the block's id
   * @return a compound tag for a specific block id
   */
  def compoundTagFromBlockId(blockId: Int): CompoundTag = {
    val defaultStateId = (blocks find { _.blockId == blockId } flatMap { _.states.find(_.default) } map { _.id }).get
    compoundTagList(defaultStateId)
  }

  /**
   * Retrieve the block structure from a given compound tag
   * @param compoundTag the compound tag of the desired block
   * @return a block from the specified compound tag
   */
  def blockFromCompoundTag(compoundTag: CompoundTag): Block = blockFromStateId(stateIdFromCompoundTag(compoundTag))

  /**
   * Retrieve the block state id from a given compound tag
   * @param compoundTag the compound tag of the desired block state
   * @return a block state id from the specified compound tag
   */
  def stateIdFromCompoundTag(compoundTag: CompoundTag): Int = {
    val stateId = compoundTagList.indexOf(compoundTag)
    assert(stateId >= 0)
    stateId
  }

  /**
   * Retrieve the block from a given block state id
   * @param stateId the state id of the desired block
   * @return a block from the specified block state id
   */
  def blockFromStateId(stateId: BlockStateId): Block = blocks find { _.states.exists(_.id == stateId) } get

  /**
   * Retrieve the block from a given block id
   * @param blockId the id of the desired block
   * @return a block from the specified block id
   */
  def blockFromBlockId(blockId: Int): Block = blocks(blockId)

  /**
   * @return a map from every solid block to a list of block state id
   */
  def blocksMap: Map[Identifier, List[BlockStateId]] = blocksTags filter { case (id, _) =>
    !fluids.contains(id)
  }

  /**
   * @return a map from every fluid block to a list of block state id
   */
  def fluidsMap: Map[Identifier, List[BlockStateId]] = blocksTags filter { case (id, _) =>
    fluids.contains(id)
  }

}