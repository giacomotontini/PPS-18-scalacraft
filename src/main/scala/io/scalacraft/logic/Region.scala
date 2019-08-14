package io.scalacraft.logic

import akka.actor.{Actor, ActorLogging, Props}
import io.scalacraft.loaders.{Blocks, Chunks, Items}
import io.scalacraft.logic.messages.Message.{BlockBreakAtPosition, BlockPlacedByUser, RequestChunkData}
import io.scalacraft.misc.Helpers
import io.scalacraft.packets.DataTypes.Position
import io.scalacraft.packets.clientbound.PlayPackets.ChunkData
import net.querz.nbt.mca.{MCAFile, MCAUtil}

class Region(mca: MCAFile) extends Actor with ActorLogging {

  override def receive: Receive = {
    case RequestChunkData(chunkX, chunkZ, fullChunk) =>
      val chunk = mca.getChunk(chunkX, chunkZ)
      val (data, primaryBitMask) = Chunks.buildChunkDataStructureAndBitmask(chunk)
      val entities = Helpers.listTagToList(chunk.getEntities)

      val chunkData = ChunkData(chunkX, chunkZ, fullChunk, primaryBitMask, data, entities)
      sender ! chunkData
    case BlockBreakAtPosition(position, _) =>
      val (chunkX, chunkZ) = (MCAUtil.blockToChunk(position.x), MCAUtil.blockToChunk(position.z))
      val compound = mca.getChunk(chunkX, chunkZ).getBlockStateAt(position.x, position.y, position.z)
      println("block break", position)
      sender ! Blocks.stateIdFromCompoundTag(compound)

    case BlockPlacedByUser(playerBlockPlacement, itemId, username) =>
      val x = Math.round(playerBlockPlacement.position.x + playerBlockPlacement.cursorPositionX)
      val y = Math.round(playerBlockPlacement.position.y + playerBlockPlacement.cursorPositionY)
      val z = Math.round(playerBlockPlacement.position.z + playerBlockPlacement.cursorPositionZ)
      val position = Position(x,y,z)
      val item = Items.getStorableItemById(itemId)
      val tag = Blocks.compoundTagFromBlockName(item.name)
      println("block place", playerBlockPlacement)
      val (chunkX, chunkZ) = (MCAUtil.blockToChunk(position.x), MCAUtil.blockToChunk(position.z))
      mca.getChunk(chunkX, chunkZ).setBlockStateAt(position.x, position.y, position.z, tag, false)
  }

}

object Region {

  def props(mca: MCAFile): Props = Props(new Region(mca))
  def name(x: Int, z: Int): String = s"region$x,$z"

}
