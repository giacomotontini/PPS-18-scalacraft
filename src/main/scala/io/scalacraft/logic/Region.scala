package io.scalacraft.logic

import akka.actor.{Actor, ActorLogging, Props}
import io.scalacraft.loaders.Chunks
import io.scalacraft.logic.messages.Message.RequestChunkData
import io.scalacraft.misc.Helpers
import io.scalacraft.packets.clientbound.PlayPackets.ChunkData
import net.querz.nbt.mca.MCAFile

class Region(mca: MCAFile) extends Actor with ActorLogging {

  override def receive: Receive = {
    case RequestChunkData(chunkX, chunkZ, fullChunk) =>
      val chunk = mca.getChunk(chunkX, chunkZ)
      val (data, primaryBitMask) = Chunks.buildChunkDataStructureAndBitmask(chunk)
      val entities = Helpers.listTagToList(chunk.getEntities)

      val chunkData = ChunkData(chunkX, chunkZ, fullChunk, primaryBitMask, data, entities)
      sender ! chunkData
  }

}

object Region {

  def props(mca: MCAFile): Props = Props(new Region(mca))
  def name(x: Int, z: Int): String = s"region$x,$z"

}
