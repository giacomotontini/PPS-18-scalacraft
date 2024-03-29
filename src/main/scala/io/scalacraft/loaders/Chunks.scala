package io.scalacraft.loaders

import java.io.{ByteArrayOutputStream, DataOutputStream}

import io.scalacraft.core.marshalling.Marshallers.{IntMarshaller, LongMarshaller}
import io.scalacraft.misc.Helpers.writeVarInt
import net.querz.nbt.mca.Chunk
import net.querz.nbt.{CompoundTag, ListTag}

/**
 * Contains utility def for chunks
 */
object Chunks {

  private val MaxSections = 16 //maximum number of section for a chunk column

  /**
   * Build the ChunkData structure and a correspondent bitmask from a chunk column.
   * @see [[https://wiki.vg/Protocol#Chunk_Data ChunkData structure]]
   * @param chunkColumn the chunk column from which to build data
   * @return a ChunkData structure in byte and the relative bitmask
   */
  def buildChunkDataStructureAndBitmask(chunkColumn: Chunk): (Array[Byte], Int) = {
    def writePaletteArray(palette: ListTag[CompoundTag])(implicit dataOutputStream: DataOutputStream): Unit = {
      writeVarInt(palette.size, dataOutputStream)
      palette.forEach(paletteName => writeVarInt(Blocks.stateIdFromCompoundTag(paletteName), dataOutputStream))
    }

    def writeLongArray(dataArray: Array[Long], valueMarshaller: LongMarshaller)
                      (implicit dataOutputStream: DataOutputStream): Unit = {
      writeVarInt(dataArray.length, dataOutputStream)
      dataArray.foreach(value => valueMarshaller.marshal(value)(dataOutputStream))
    }

    val byteArrayOutputStream = new ByteArrayOutputStream()
    implicit val dataOutputStream: DataOutputStream = new DataOutputStream(byteArrayOutputStream)
    var bitmask: Int = 0
    val valueMarshaller = new LongMarshaller()
    val intMarshaller = new IntMarshaller()

    for (i <- 0 until MaxSections) {
      val chunkSection = chunkColumn.getSection(i)

      if (chunkSection != null && !chunkSection.isEmpty) {
        bitmask |= 1 << i
        val numberOfBytes = chunkSection.getPaletteBitSize
        val palette = chunkSection.getPalette
        val dataArray = chunkSection.getBlockStates
        val blockLight = chunkSection.getBlockLight
        val skyLight = chunkSection.getSkyLight
        dataOutputStream.writeByte(numberOfBytes)
        writePaletteArray(palette)
        writeLongArray(dataArray, valueMarshaller)
        dataOutputStream.write(blockLight)
        dataOutputStream.write(skyLight)
      }
    }

    chunkColumn.getBiomes.foreach(biome => intMarshaller.marshal(biome))
    dataOutputStream.close()
    (byteArrayOutputStream.toByteArray, bitmask)
  }

}
