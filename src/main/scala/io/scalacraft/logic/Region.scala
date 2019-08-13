package io.scalacraft.logic

import akka.actor.{Actor, ActorLogging, Props}
import io.scalacraft.loaders.Chunks
import io.scalacraft.logic.messages.Message.{RequestChunkData, RequestSpawnPoints}
import io.scalacraft.misc.Helpers._
import io.scalacraft.packets.DataTypes.Position
import io.scalacraft.packets.clientbound.PlayPackets.ChunkData
import net.querz.nbt.{CompoundTag, StringTag}
import net.querz.nbt.mca.{Chunk, MCAFile, MCAUtil}

class Region(mca: MCAFile) extends Actor with ActorLogging {
  private[this] def firstSpawnableHeight(chunk: Chunk, x: Int, z: Int): Int = {
    var yIndex = 0
    var lastIndexWithoutAir = 0
    val airTag = new CompoundTag()
    airTag.put("Name", new StringTag("minecraft:air"))
    while (yIndex < 255 && chunk.getBlockStateAt(x, yIndex, z) != null) {
      if(!chunk.getBlockStateAt(x, yIndex, z).equals(airTag))
        lastIndexWithoutAir = yIndex
      yIndex += 1
    }
    lastIndexWithoutAir
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
        val posX = MCAUtil.chunkToBlock(chunkX)+x
        val posZ = MCAUtil.chunkToBlock(chunkZ)+z
        val y = firstSpawnableHeight(chunkColumn, x, z)
        val biome = chunkColumn.getBiomeAt(x, z)
        val isWater = chunkColumn.getBlockStateAt(x, y, z).isWater()
        println(chunkColumn.getBlockStateAt(x, y, z)," ", isWater)
        biome -> (Position(posX, y, posZ), isWater)
      }).groupBy(_._1).map {
        case (biomeIndex, values) => biomeIndex -> values.map(_._2).toList
      }
      sender ! biomeToSpawnPosition
  }

}

object Region {

  def props(mca: MCAFile): Props = Props(new Region(mca))

  def name(x: Int, z: Int): String = s"region$x,$z"

}
