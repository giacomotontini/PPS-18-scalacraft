package io.scalacraft.logic

import akka.actor.{Actor, ActorLogging, Props}
import io.scalacraft.loaders.{Blocks, Chunks}
import io.scalacraft.logic.messages.Message.{BlockBreakAtPosition, RequestChunkData}
import io.scalacraft.misc.Helpers
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
      println("region", Blocks.stateIdFromCompoundTag(compound))
      sender ! Blocks.stateIdFromCompoundTag(compound)
  }

}

object Region {

  def props(mca: MCAFile): Props = Props(new Region(mca))
  def name(x: Int, z: Int): String = s"region$x,$z"

}
