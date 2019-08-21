package io.scalacraft.logic

import akka.actor.{Actor, ActorLogging, Props}
import io.scalacraft.loaders.{Blocks, Chunks}
import io.scalacraft.logic.messages.Message._
import io.scalacraft.misc.Helpers
import io.scalacraft.packets.DataTypes.Position
import io.scalacraft.packets.clientbound.PlayPackets.ChunkData
import net.querz.nbt.CompoundTag
import net.querz.nbt.mca.MCAFile

class Region(mca: MCAFile) extends Actor with ActorLogging {

  private val airTag = Blocks.defaultCompoundTagFromName("air").get

  private var cleanupNeeded: Boolean = false

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
