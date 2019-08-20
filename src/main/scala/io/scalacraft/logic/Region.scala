package io.scalacraft.logic

import akka.actor.{Actor, ActorLogging, Props}
import io.scalacraft.ComputeMoves
import io.scalacraft.loaders.Chunks
import io.scalacraft.logic.messages.Message.{Height, RequestChunkData, RequestNearbyPoints, RequestSpawnPoints}
import io.scalacraft.misc.Helpers._
import io.scalacraft.packets.DataTypes.Position
import io.scalacraft.packets.clientbound.PlayPackets.ChunkData
import net.querz.nbt.mca.{Chunk, MCAFile, MCAUtil}

class Region(mca: MCAFile) extends Actor with ActorLogging {

  private[this] def firstSpawnableHeight(chunk: Chunk, x: Int, z: Int): Int = {
    var yIndex = 255
    while (chunk.getBlockStateAt(x, yIndex, z) == null || !chunk.getBlockStateAt(x, yIndex, z).isSpawnableSurface) {
      yIndex -= 1
    }
    yIndex + 1
  }

  private[this] def cubeStates(chunk: Chunk, blockX: Int, blockY: Int, blockZ: Int): Seq[String] = {
    for(yDrift <- -2 to 1) yield {
      val nbtBlockState = chunk.getBlockStateAt(blockX,blockY+yDrift,blockZ)
      val blockState = if(nbtBlockState.isSpawnableSurface) "surface" else if(nbtBlockState.emptyCube) "noSurface" else "unused"
      s"state($blockX,${blockY+yDrift},$blockZ,$blockState)"
    }
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
    case RequestNearbyPoints(x:Int, y:Int, z:Int, oldX:Int, oldZ:Int) =>
      val chunk = mca.getChunk(MCAUtil.blockToChunk(x), MCAUtil.blockToChunk(z))
      val forbiddendXZPair = (oldX-x,oldZ-z)
      var cubeStatesAssertions = Seq[String]()
      for(xzPair <- List((-1,0),(1,0),(0,1),(0,-1))
          if !xzPair.equals(forbiddendXZPair)){
        val states = cubeStates(chunk, x+xzPair._1, y, z+xzPair._2)
        cubeStatesAssertions ++= states
      }
      val computeCreaturesMoves = new ComputeCreatureMoves(cubeStatesAssertions)
      sender ! computeCreaturesMoves.computeMoves(x,y,z)
    case Height(x:Int, y:Int, z:Int) =>
      val chunk = mca.getChunk(MCAUtil.blockToChunk(x), MCAUtil.blockToChunk(z))
      val computeMoves = new ComputeMoves("src/main/resources/computeMoves.pl")
      var cubeStatesAssertions = Seq[String]()
      for(xzPair <- List((-1,0),(1,0),(0,1),(0,-1))){
        val states = cubeStates(chunk, x+xzPair._1, y, z+xzPair._2)
        cubeStatesAssertions ++= states
      }
      computeMoves.assertions(cubeStatesAssertions)
      sender ! computeMoves.computeMoves(x,y,z)
  }
}

object Region {

  def props(mca: MCAFile): Props = Props(new Region(mca))

  def name(x: Int, z: Int): String = s"region$x,$z"

}
