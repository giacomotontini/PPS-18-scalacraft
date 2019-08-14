package io.scalacraft.misc

import java.io.{InputStream, OutputStream}

import io.scalacraft.packets.DataTypes.VarInt
import net.querz.nbt.{CompoundTag, ListTag, StringTag, Tag}

import scala.collection.JavaConverters._

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

  def linearCongruentialGenerator(rseed: Int): Iterator[Int] = new Iterator[Int] {
    var seed: Int = rseed

    override def hasNext: Boolean = true

    override def next: Int = {
      seed = (seed * 1103515245 + 12345) & Int.MaxValue;
      seed
    }
  }

  def listTagToList[T <: Tag[_]](listTag: ListTag[T]): List[T] = listTag.iterator().asScala.toList

  implicit class RichNbtCompoundTag(nbtTag: CompoundTag) {
    private[this] def checkIfIsASpecificTypeOfBlock(tagValue: String):Boolean = {
      val waterStringTag = new StringTag()
      waterStringTag.setValue(tagValue)
      nbtTag.entrySet().stream().anyMatch(elem => elem.getValue.equals(waterStringTag))
    }
    def isWater(): Boolean = {
     checkIfIsASpecificTypeOfBlock("minecraft:water")
    }
    def isAir(): Boolean = {
      checkIfIsASpecificTypeOfBlock("minecraft:air") ||
        checkIfIsASpecificTypeOfBlock("minecraft:cave_air")
    }
    def isGrass(): Boolean = {
        checkIfIsASpecificTypeOfBlock("minecraft:grass") ||
          checkIfIsASpecificTypeOfBlock("minecraft:tall_grass")
    }
    def isLeaves(): Boolean = {
      checkIfIsASpecificTypeOfBlock("minecraft:birch_leaves") ||
        checkIfIsASpecificTypeOfBlock("minecraft:oak_leaves")
    }
    def isWoodOfTree(): Boolean = {
      checkIfIsASpecificTypeOfBlock("minecraft:birch_log") ||
        checkIfIsASpecificTypeOfBlock("minecraft:oak_log")
    }
    def isSpawnableSurface(): Boolean = {
      !isAir() && !isWoodOfTree() && !isLeaves() && !isGrass()
    }
  }

}
