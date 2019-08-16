package io.scalacraft.logic

import akka.actor.{Actor, ActorLogging, Props}
import io.scalacraft.loaders.Chunks
import io.scalacraft.logic.messages.Message.{RequestChunkData, RequestProbabilisticSpawnPositionsForBiomes}
import io.scalacraft.misc.Helpers._
import io.scalacraft.packets.DataTypes.Position
import io.scalacraft.packets.clientbound.PlayPackets.ChunkData
import net.querz.nbt.mca.{Chunk, MCAFile, MCAUtil}

import scala.util.Random


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

    case RequestProbabilisticSpawnPositionsForBiomes(chunkX, chunkZ, biomeIndexesToSpawnProbability) =>
      val randomGenerator: Random.type = scala.util.Random
      val chunkColumn = mca.getChunk(chunkX, chunkZ)
      val spawnPositions =
        (for(x <- 0 to 15;
          z <- 0 to 15
            if biomeIndexesToSpawnProbability.keySet.contains(chunkColumn.getBiomeAt(x, z)) &&
              randomGenerator.nextFloat() < biomeIndexesToSpawnProbability(chunkColumn.getBiomeAt(x, z))/256) yield {
          val posX = MCAUtil.chunkToBlock(chunkX) + x
          val posZ = MCAUtil.chunkToBlock(chunkZ) + z
          val posY = firstSpawnableHeight(chunkColumn, x, z)
          val isWater = chunkColumn.getBlockStateAt(posX, posY-1, posZ).isWater()
          (Position(posX, posY, posZ), isWater)
        }).toList
      sender ! spawnPositions
  }

}

object Region {

  def props(mca: MCAFile): Props = Props(new Region(mca))

  def name(x: Int, z: Int): String = s"region$x,$z"

}
