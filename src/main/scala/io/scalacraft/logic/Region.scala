package io.scalacraft.logic

import akka.actor.{Actor, ActorLogging, Props}
import io.scalacraft.loaders.Chunks
import io.scalacraft.logic.messages.Message.{Height, RequestChunkData, RequestNearbyPoints, RequestSpawnPoints}
import io.scalacraft.misc.Helpers._
import io.scalacraft.packets.DataTypes.Position
import io.scalacraft.packets.clientbound.PlayPackets.ChunkData
import net.querz.nbt.mca.{Chunk, MCAFile, MCAUtil}

class Region(mca: MCAFile) extends Actor with ActorLogging {
  /*private[this] def tmp(chunk: Chunk, x: Int, z: Int, from: Int = 255, by: Int = -1): Int = {
    require(from < 255 && from > 0)
    require(by == -1 || by == 1)
    var yIndex = from
    val condition : Int => Boolean = y =>
      if(by == -1)
      chunk.getBlockStateAt(x, y, z) == null || !chunk.getBlockStateAt(x, y, z).isSpawnableSurface()
    else
        chunk.getBlockStateAt(x, y, z) != null && chunk.getBlockStateAt(x, y, z).isSpawnableSurface()
    while (condition(yIndex)) {
      yIndex += by
    }
    if(by == -1) yIndex + 1 else yIndex
  }*/
  var print: Boolean = true
  private[this] def firstSpawnableHeight(chunk: Chunk, x: Int, z: Int): Int = {
    var yIndex = 255
    while (chunk.getBlockStateAt(x, yIndex, z) == null || !chunk.getBlockStateAt(x, yIndex, z).isSpawnableSurface) {
      yIndex -= 1
    }
    yIndex + 1
  }

  private[this] def safeMove(chunk: Chunk, x: Int, y: Int, z: Int): Option[Position] = {
    if (chunk.getBlockStateAt(x, y , z).isSpawnableSurface &&
      (chunk.getBlockStateAt(x, y+1,z)==null || chunk.getBlockStateAt(x, y + 1, z).isAir || chunk.getBlockStateAt(x, y + 1,z).areFlowers)) {
      Some(Position(x, y + 1, z))
    }  else if (chunk.getBlockStateAt(x, y , z).isAir && chunk.getBlockStateAt(x, y-1, z).isAir && chunk.getBlockStateAt(z, y - 2, z).isSpawnableSurface) {
      Some(Position(x, y - 1, z))
    }else if((chunk.getBlockStateAt(x, y , z).isAir && chunk.getBlockStateAt(x, y-1, z).isSpawnableSurface) || chunk.getBlockStateAt(x, y , z).areFlowers) {
      Some(Position(x,y,z))
    }
    else
      None
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
        val posY = firstSpawnableHeight(chunkColumn, x, z)
        val biome = chunkColumn.getBiomeAt(x, z)
        val isWater = chunkColumn.getBlockStateAt(x, posY - 1, z).isWater
        biome -> (Position(posX, posY, posZ), isWater)
      }).groupBy(_._1).map {
        case (biomeIndex, values) =>
          biomeIndex -> values.map(_._2).toSet
      }
      sender ! biomeToSpawnPosition
    case RequestNearbyPoints(x:Int, y:Int, z:Int, oldX:Int, oldY:Int, oldZ:Int) =>
      val chunk = mca.getChunk(MCAUtil.blockToChunk(x), MCAUtil.blockToChunk(z))
      val toCheck = List(Position(-1,0,0), Position(1,0,0), Position(0,0,-1), Position(0,0,1))
      val forbiddenPosition = Position(oldX-x, oldY-y, oldZ-z)
      sender ! toCheck.collect{
       case position @ Position(posX,posY,posZ) if !position.equals(forbiddenPosition) =>
          safeMove(chunk, x+posX, y+posY, z+posZ)
      }.collect{
        case Some(position) => position
      }
    case Height(x:Int, y:Int, z:Int) =>
      val chunk = mca.getChunk(MCAUtil.blockToChunk(x), MCAUtil.blockToChunk(z))
      val toCheck = List(Position(-1,0,0), Position(1,0,0), Position(0,0,-1), Position(0,0,1))
      sender ! toCheck.collect{
        case Position(posX,posY,posZ) =>
          safeMove(chunk, x+posX, y+posY, z+posZ)
      }.collect{
        case Some(position) => position
      }
  }

}

object Region {

  def props(mca: MCAFile): Props = Props(new Region(mca))

  def name(x: Int, z: Int): String = s"region$x,$z"

}
