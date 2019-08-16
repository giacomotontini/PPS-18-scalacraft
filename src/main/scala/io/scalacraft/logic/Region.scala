package io.scalacraft.logic

import akka.actor.{Actor, ActorLogging, Props}
import io.scalacraft.loaders.Chunks
import io.scalacraft.logic.messages.Message.{RequestChunkData, RequestSpawnPoints}
import io.scalacraft.misc.Helpers._
import io.scalacraft.packets.DataTypes.Position
import io.scalacraft.packets.clientbound.PlayPackets.ChunkData
import net.querz.nbt.{CompoundTag, StringTag}
import net.querz.nbt.mca.{Chunk, MCAFile, MCAUtil}

import scala.collection.mutable.ListBuffer

class Region(mca: MCAFile) extends Actor with ActorLogging {
  private[this] def firstSpawnableHeight(chunk: Chunk, x: Int, z: Int): Int = {
    var yIndex = 255
    while (chunk.getBlockStateAt(x, yIndex, z) == null || !chunk.getBlockStateAt(x, yIndex, z).isSpawnableSurface()) {
      yIndex -= 1
    }
    yIndex + 1
  }

  override def receive: Receive = {
    case RequestChunkData(chunkX, chunkZ, fullChunk) =>
      val chunk = mca.getChunk(chunkX, chunkZ)
      val (data, primaryBitMask) = Chunks.buildChunkDataStructureAndBitmask(chunk)
      val entities = listTagToList(chunk.getEntities)
      val chunkData = ChunkData(chunkX, chunkZ, fullChunk, primaryBitMask, data, entities)
      sender ! chunkData

    case RequestSpawnPoints(chunkX, chunkZ) =>
      val chunkColumn = mca.getChunk(chunkX, chunkZ)
      val biomeToSpawnPosition = (for (x <- 0 to 15;
                                       z <- 0 to 15) yield {
        val posX = MCAUtil.chunkToBlock(chunkX) + x
        val posZ = MCAUtil.chunkToBlock(chunkZ) + z
        val y = firstSpawnableHeight(chunkColumn, x, z)
        val biome = chunkColumn.getBiomeAt(x, z)
        val isWater = chunkColumn.getBlockStateAt(x, y-1, z).isWater()
        biome -> (Position(posX, y, posZ), isWater)
      }).groupBy(_._1).map {
        case (biomeIndex, values) =>
          biomeIndex ->  values.map(_._2).toSet
      }
      sender ! biomeToSpawnPosition
  }

}

object Region {

  def props(mca: MCAFile): Props = Props(new Region(mca))

  def name(x: Int, z: Int): String = s"region$x,$z"

}
