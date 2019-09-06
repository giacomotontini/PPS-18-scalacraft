package io.scalacraft.core.marshalling

import java.lang.reflect.Field

import io.scalacraft.core.marshalling.annotations.indexType
import io.scalacraft.core.packets.Entities.{MobEntity, ObjectEntity}

/**
 * Base class for entities. Add some useful method for serialization and deserialization.
 */
class EntityMetadata {

  private val fields: List[Field] = {
    def _listField(clazz: Class[_]): List[Field] = clazz match {
      case _ if clazz != classOf[EntityMetadata] => _listField(clazz.getSuperclass) ::: clazz.getDeclaredFields.toList
      case _ => List.empty
    }

    val _fields = _listField(getClass)
    _fields foreach {
      _.setAccessible(true)
    }
    _fields
  }

  /**
   * Set all fields from a list of values.
   *
   * @param values list of values for all fields
   */
  private[marshalling] def setValues(values: List[Any]): Unit = for (i <- values.indices) {
    fields(i).set(this, values(i))
  }

  /**
   * List of index type for all fields.
   */
  private[marshalling] val indexes: List[Int] = fields map (_.getDeclaredAnnotation(classOf[indexType]).index())

  /**
   * List of values for all fields.
   *
   * @return a list of values
   */
  private[marshalling] def values: List[Any] = fields map (_ get this)

  // for test purpose: needed because every unmarshalled packet is constructed via reflection throw a class constructor
  // and instance references are different
  override def equals(o: Any): Boolean = o match {
    case other: EntityMetadata => other.values equals this.values
    case _ => false
  }

  override def toString: String = (new StringBuilder append getClass.getSimpleName)
    .append("[type: ")
    .append(if (this.isInstanceOf[ObjectEntity]) "ObjectEntity" else "")
    .append(if (this.isInstanceOf[MobEntity]) "MobEntity" else "")
    .append("] {")
    .append(fields map { f => s"${f.getName}: ${f get this}" } mkString ", ")
    .append("}")
    .toString

}
