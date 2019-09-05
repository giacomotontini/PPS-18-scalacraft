package io.scalacraft.misc

import java.io.{InputStream, OutputStream}

import io.scalacraft.core.packets.DataTypes.{Angle, Position, VarInt}
import net.querz.nbt.{ListTag, Tag}

import scala.collection.JavaConverters._
import scala.util.Random

/**
 * Contains some useful utility methods.
 */
object Helpers {

  private val random = new Random

  /**
   * Convert an hex string to an array of bytes.
   *
   * @param hex the hex string
   * @return the array of bytes
   */
  def hex2bytes(hex: String): Array[Byte] = hex.replaceAll("[^0-9A-Fa-f]", "")
    .sliding(2, 2).toArray.map(Integer.parseInt(_, 16).toByte)

  /**
   * Convert an array of bytes to an hex string.
   *
   * @param bytes the array of bytes
   * @param sep an optional separator
   * @return the hex string
   */
  def bytes2hex(bytes: Array[Byte], sep: Option[String] = None): String =
    bytes.map("%02x".format(_)).mkString(sep.getOrElse(""))

  /**
   * Read a variable integer from a stream.
   *
   * @param inStream the input stream from which the variable integer must be read
   * @return the [[io.scalacraft.packets.DataTypes.VarInt VarInt]] wrapper
   */
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

  /**
   * Write a variable integer in a stream.
   *
   * @param value the value to be written
   * @param outStream the output stream in which the variable integer must be written
   * @return the length of the variable integer written
   */
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

  /**
   * Create a linear congruential generator from a seed.
   *
   * @param rseed the initial seed
   * @return an iterator which represent the generator
   */
  def linearCongruentialGenerator(rseed: Int): Iterator[Int] = new Iterator[Int] {
    var seed: Int = rseed

    override def hasNext: Boolean = true

    override def next: Int = {
      seed = (seed * 1103515245 + 12345) & Int.MaxValue
      seed
    }
  }

  /**
   * Convert a [[net.querz.nbt.ListTag ListTag]] to a list of tags.
   *
   * @param listTag the [[net.querz.nbt.ListTag ListTag]] to be converted
   * @return the list of tags converted
   */
  def listTagToList[T <: Tag[_]](listTag: ListTag[T]): List[T] = listTag.iterator().asScala.toList

  /**
   * Create a random angle.
   *
   * @return the random angle
   */
  def randomAngle: Angle = Angle(random.nextInt() % 256)

  /**
   * Create a random velocity.
   *
   * @return the random velocity
   */
  def randomVelocity: Int = random.nextInt(4096) - 2048

  /**
   * The relative coordinates adjacent to `Position(0,0,0)`
   */
  val RelativeNears = List(Position(0, -1, 0), Position(0, 1, 0), Position(0, 0, -1), Position(0, 0, 1),
    Position(-1, 0, 0), Position(1, 0, 0))

}
