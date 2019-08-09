package io.scalacraft.misc

import java.io.{ByteArrayOutputStream, DataOutputStream, InputStream, OutputStream}

import io.scalacraft.core.marshalling.Marshallers.LongMarshaller
import io.scalacraft.packets.DataTypes.VarInt
import net.querz.nbt.mca.Chunk
import net.querz.nbt.{CompoundTag, ListTag}

private[scalacraft] object Helpers {

  def hex2bytes(hex: String): Array[Byte] = {
    hex.replaceAll("[^0-9A-Fa-f]", "")
      .sliding(2, 2).toArray.map(Integer.parseInt(_, 16).toByte)
  }

  def bytes2hex(bytes: Array[Byte], sep: Option[String] = None): String =
    bytes.map("%02x".format(_)).mkString(sep.getOrElse(""))

  def readVarInt(inStream: InputStream): VarInt = {
    var numRead = 0
    var result = 0
    var read = 0
    do {
      read = inStream.read()
      result |= ((read & 0x7f) << (7 * numRead))
      numRead += 1
      if (numRead > 5) {
        throw new IllegalArgumentException("VarInt is too big")
      }
    } while ((read & 0x80) != 0)

    VarInt(result, numRead)
  }

  def writeVarInt(value: Int, outStream: OutputStream): Int = {
    var i = value
    var numWrite = 0
    do {
      var temp = i & 0x7f
      i = i >>> 7
      if (i != 0) {
        temp |= 0x80
      }
      numWrite += 1
      outStream.write(temp)
    } while (i != 0)

    numWrite
  }

  def buildChunkDataStructureAndBitmask(chunkColumn: Chunk): (Array[Byte], Int) = {
    def writePaletteArray(palette: ListTag[CompoundTag])(implicit dataOutputStream:DataOutputStream): Unit = {
      writeVarInt(palette.size, dataOutputStream)
      palette.forEach(paletteName => writeVarInt(Blocks.idFromCompoundTag(paletteName),dataOutputStream))
    }
    def writeLongArray(dataArray: Array[Long], valueMarshaller: LongMarshaller)(implicit dataOutputStream: DataOutputStream): Unit = {
      writeVarInt(dataArray.length, dataOutputStream)
      dataArray.foreach(value => valueMarshaller.marshal(value)(dataOutputStream))
    }
    def writeByteArray(dataArray: Array[Byte])(implicit dataOutputStream: DataOutputStream): Unit = {
      writeVarInt(dataArray.length, dataOutputStream)
      dataArray.foreach(dataArrayElement => dataOutputStream.write(dataArrayElement))
    }
    val byteArrayOutputStream = new ByteArrayOutputStream()
    implicit val dataOutputStream: DataOutputStream = new DataOutputStream(byteArrayOutputStream)
    var bitmask: Int = 0
    val valueMarshaller = new LongMarshaller()
    for(i <- 0 to 15) {
      val chunkSection = chunkColumn.getSection(i)
      println(i, chunkSection)
      if(chunkSection != null && !chunkSection.isEmpty) {
        bitmask |= 1 << i
        val numberOfBytes = chunkSection.getPaletteBitSize
        val palette = chunkSection.getPalette
        val dataArray = chunkSection getBlockStates
        val blockLight = chunkSection getBlockLight
        val skyLight = chunkSection.getSkyLight
        dataOutputStream.write(numberOfBytes)
        writePaletteArray(palette)
        writeLongArray(dataArray,valueMarshaller)
        writeByteArray(blockLight)
        writeByteArray(skyLight)
      }
    }
    println(bitmask)
    chunkColumn.getBiomes.foreach(biome => dataOutputStream.write(biome))
    dataOutputStream.close()
    (byteArrayOutputStream.toByteArray,bitmask)
 }
}
