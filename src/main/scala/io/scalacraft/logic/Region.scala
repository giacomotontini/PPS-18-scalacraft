package io.scalacraft.logic

import akka.actor.{Actor, ActorLogging, Props}
import io.scalacraft.loaders.{Blocks, Chunks}
import io.scalacraft.logic.commons.Message.{RequestChunkData, RequestNearbyPoints, RequestSpawnPoints, _}
import io.scalacraft.logic.creatures.misc.ComputeCreatureMoves
import io.scalacraft.misc.Helpers
import io.scalacraft.misc.Helpers._
import io.scalacraft.packets.DataTypes.Position
import io.scalacraft.packets.clientbound.PlayPackets.ChunkData
import net.querz.nbt.CompoundTag
import net.querz.nbt.mca.{Chunk, MCAFile, MCAUtil}

class Region(mca: MCAFile) extends Actor with ActorLogging {

  private val airTag = Blocks.defaultCompoundTagFromName("air").get

  private var cleanupNeeded: Boolean = false

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
      if (cleanupNeeded) chunk.cleanupPalettesAndBlockStates()

      sender ! (if (chunk == null) ChunkNotPresent else {
        val (data, primaryBitMask) = Chunks.buildChunkDataStructureAndBitmask(chunk)
        val entities = Helpers.listTagToList(chunk.getEntities)

        ChunkData(chunkX, chunkZ, fullChunk, primaryBitMask, data, entities)
      })

    case RequestBlockState(Position(x, y, z)) => sender ! mca.getChunk(x >> 4, z >> 4).getBlockStateAt(x, y, z)

    case ChangeBlockState(Position(x, y, z), tag) =>
      mca.getChunk(x >> 4, z >> 4).setBlockStateAt(x, y, z, tag, false)
      cleanupNeeded = true

    case FindFirstSolidBlockPositionUnder(Position(x, y, z)) =>
      val chunk = mca.getChunk(x >> 4, z >> 4)
      var currentY = y
      while (!isSolidBlock(chunk.getBlockStateAt(x, currentY, z))) currentY -= 1
      sender ! Position(x, currentY, z)

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

    case unhandled => log.warning(s"Unhandled message in Region: $unhandled")
  }

  private def isSolidBlock(tag: CompoundTag): Boolean = tag match {
    case `airTag` => false
    case null => false
    case _ => true
  }

}

object Region {

  def props(mca: MCAFile): Props = Props(new Region(mca))

  def name(x: Int, z: Int): String = s"region$x,$z"

}
