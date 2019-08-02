package io.scalacraft.core

import java.lang.reflect.Field

import io.scalacraft.core.Entities.Entity

class EntityMetadata {

  // TODO: change visibility

  private val fields: List[Field] = {
    def _listField(clazz: Class[_]): List[Field] = clazz match {
      case _ if clazz != classOf[EntityMetadata] => _listField(clazz.getSuperclass) ::: clazz.getDeclaredFields.toList
      case _ => List.empty
    }

    val _fields = _listField(getClass)
    _fields foreach { _.setAccessible(true) }
    _fields
  }

  private val _indexes: List[_] = {
    fields.foreach(f  => println(f.getDeclaredAnnotations.toList))
    null
  }

  private[scalacraft] def setValues(values: List[Any]): Unit = for (i <- values.indices) {
    fields(i).set(this, values(i))
  }

  private[scalacraft] def values: List[Any] = fields map { _.get(this) }
  private[scalacraft] def indexes: List[_] = _indexes


}

object l extends App {
  println(new Entity().indexes.toList)
}
