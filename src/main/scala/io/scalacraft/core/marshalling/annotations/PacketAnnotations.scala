package io.scalacraft.core.marshalling.annotations

import scala.annotation.StaticAnnotation

/**
 * Contains all packet's annotation
 */
private[core] object PacketAnnotations {
  val MaxStringLength: Int = 32767 //default

  /**
   * Used to indicate that a class is a packet
   * @param id the id of the packet
   */
  class packet(id: Int) extends StaticAnnotation

  /**
   * Used to indicate that a field is a variable length field. (VarInt, VarLong)
   */
  class boxed extends StaticAnnotation

  /**
   * Used to indicate that an Int field is to be considered as a byte
   */
  class byte extends StaticAnnotation

  /**
   * Used to indicate that an Int field is to be considered as a short int
   */
  class short extends StaticAnnotation

  /**
   * Used to indicate the maximum length for a string field
   * @param value the max length
   */
  class maxLength(value: Int) extends StaticAnnotation

  /**
   * Used to mark a field as "to be replaced" with another
   * @tparam K the class type of the field in the current packet
   *           which determines which class must be used to replace this field
   */
  class switchType[K] extends StaticAnnotation

  /**
   * Used to indicate that a class belong to a group of replaceable field and has an unique value.
   * @param value determine the value of the marked class.
   *              When a field need to be replaced, this is the value
   *              compared with the one indicated in a packet field.
   */
  class switchKey(value: Any) extends StaticAnnotation

  /**
   * Used to indicate that a field is preceded by another field of type T.
   * Preceded could be combined with from context to alter the index of the packet to which look at
   * @tparam T the class type of the value which come before this field
   */
  class precededBy[T] extends StaticAnnotation

  /**
   * Used to indicate that a field value could only be one of the value indicated in "enumValue"s tag of type K
   * @tparam K the classes type of this enum group to look at
   */
  class enumType[K] extends StaticAnnotation

  /**
   * Used to indicate the value of the marked class as part of an enumerator group.
   * @param value the value of the marked class within enumerator group
   */
  class enumValue(value: Any) extends StaticAnnotation

  /**
   * Indicate that for a field, a value of another field located at 'index' must be considered.
   * Used in combination with other annotation or for Option field.
   * @param index the field index in this packet to look at
   */
  class fromContext(index: Int) extends StaticAnnotation

}
