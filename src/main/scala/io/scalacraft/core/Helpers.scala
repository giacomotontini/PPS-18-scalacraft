package io.scalacraft.core

import java.io.{InputStream, OutputStream}

import io.scalacraft.core.DataTypes.VarInt

object Helpers {

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
      var temp = value & 0x7f
      i = i >>> 7
      if (value != 0) {
        temp |= 0x80
      }
      numWrite += 1
      outStream.write(temp)
    } while (value != 0)

    numWrite
  }

}
