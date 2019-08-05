package io.scalacraft.core

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
        case list: List[_] => "[" + list.map( _.toString ).mkString(", ") + "]"
        case Some(value) => value
        case None => "_"
        case _ => value
      })
      stringBuilder.toString
    } mkString ", ")
    .append(" }")
    .toString

}
