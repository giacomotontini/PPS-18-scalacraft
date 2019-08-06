package io.scalacraft.core

import net.querz.nbt.{CompoundTag, Tag}

trait Structure extends Product {

  override def toString: String = (new StringBuilder).append(getClass.getSimpleName)
    .append(" { ")
    .append(getClass.getDeclaredFields map { f =>
      val stringBuilder = new StringBuilder
      stringBuilder.append(f.getName)
      f.setAccessible(true)
      val value = f.get(this)
      if (value.isInstanceOf[Option[_]]) {
        stringBuilder.append("?")
      }
      stringBuilder.append(": ")

      stringBuilder.append(value match {
        case list: List[_] => "[" + list.take(16).map( _.toString ).mkString(", ") +
          (if (list.size > 16) ", <truncated>])" else "]")
        case Some(value) => value
        case None => "_"
        case str: String => s""""$str""""
        case _: CompoundTag => "<truncated>"
        case _ => value
      })
      stringBuilder.toString
    } mkString ", ")
    .append(" }")
    .toString

}
