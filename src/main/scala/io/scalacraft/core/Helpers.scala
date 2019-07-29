package io.scalacraft.core

object Helpers {

  def hex2bytes(hex: String): Array[Byte] = {
    hex.replaceAll("[^0-9A-Fa-f]", "")
      .toSeq.sliding(2, 2)
      .map(_.unwrap).toArray
      .map(Integer.parseInt(_, 16).toByte)
  }

}
