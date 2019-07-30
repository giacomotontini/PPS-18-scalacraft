package io.scalacraft.core

import scala.reflect.runtime.universe.{TypeTag, Type, typeTag}

object Helpers {

  def runtimeType[P: TypeTag](obj: P): Type = typeTag[P].tpe

  def hex2bytes(hex: String): Array[Byte] = {
    hex.replaceAll("[^0-9A-Fa-f]", "")
      .sliding(2, 2).toArray.map(Integer.parseInt(_, 16).toByte)
  }

}
