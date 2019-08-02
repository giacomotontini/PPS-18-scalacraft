package io.scalacraft.core

import java.lang.reflect.Field

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

  private[scalacraft] def setValues(values: List[Any]): Unit = for (i <- values.indices) {
    fields(i).set(this, values(i))
  }

  private[scalacraft] val indexes: List[Int] = fields map( _.getDeclaredAnnotation(classOf[indexType]).index())
  private[scalacraft] def values: List[Any] = fields map { _.get(this) }

  //for test purpose: needed because every unmarshalled packet is constructed via reflection throw a class constructor and instance references are different
  override def equals(o: Any): Boolean = o match {
    case other: EntityMetadata => other.values equals this.values
    case _ => false
  }

}
