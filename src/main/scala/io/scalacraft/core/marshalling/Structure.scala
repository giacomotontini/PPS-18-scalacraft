package io.scalacraft.core.marshalling

import net.querz.nbt.CompoundTag

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
        case list: List[_] => prettyTraversable(list)
        case byteArray: Array[Byte] => prettyTraversable(byteArray)
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

  private def prettyTraversable[T](traversable: Traversable[T]): String =  "[" +
    traversable.take(16).map( _.toString ).mkString(", ") + (if (traversable.size > 16) ", <truncated>])" else "]")

}
