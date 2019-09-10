package io.scalacraft.core.marshalling

import java.io.{DataInputStream, DataOutputStream, EOFException}
import java.nio.charset.StandardCharsets
import java.util.UUID

import io.scalacraft.misc.Helpers
import io.scalacraft.core.packets.DataTypes.{Angle, Position}
import net.querz.nbt.Tag

import scala.collection.mutable
import scala.reflect.runtime.universe._

/**
 * Contains marshallers for all types.
 */
object Marshallers {

  /**
   * Wrap a [[java.io.DataInputStream DataInputStream]] to throw an [[java.io.EOFException EOFException]] when the end
   * of file is reached insted of return a negative value.
   *
   * @param base the data input stream
   */
  implicit class RichStream(base: DataInputStream) {
    def readIfIsAvailable(): Int = {
      val readedValue = base.read()
      if (readedValue < 0) throw new EOFException else readedValue
    }
  }

  /**
   * A marshaller for boolean type.
   *
   * @param contextFieldIndex @see [[io.scalacraft.core.marshalling.Marshaller#contextFieldIndex contextFieldIndex]]
   */
  class BooleanMarshaller(val contextFieldIndex: Option[Int] = None) extends Marshaller {
    override def marshal(obj: Any)(implicit outStream: DataOutputStream): Unit = obj match {
      case b: Boolean => outStream.write(if (b) 0x1 else 0x0)
    }

    override def internalUnmarshal()(implicit context: Context, inStream: DataInputStream): Any = {
      val bool = inStream.readIfIsAvailable() == 0x1
      context.addField(bool)
      bool
    }
  }

  /**
   * A marshaller for byte type.
   *
   * @param isUnsigned true if the marshaller should treat a byte as unsigned
   * @param contextFieldIndex @see [[io.scalacraft.core.marshalling.Marshaller#contextFieldIndex contextFieldIndex]]
   */
  class ByteMarshaller(val isUnsigned: Boolean = false, val contextFieldIndex: Option[Int] = None) extends Marshaller {
    override def marshal(obj: Any)(implicit outStream: DataOutputStream): Unit = obj match {
      case i: Int => outStream.write(i & 0xFF)
    }

    override def internalUnmarshal()(implicit context: Context, inStream: DataInputStream): Any = {
      val b = inStream.readIfIsAvailable()
      context.addField(b)
      if (isUnsigned) b else b.toByte.toInt
    }
  }

  /**
   * A marshaller for short type.
   *
   * @param isUnsigned true if the marshaller should treat a short as unsigned
   * @param contextFieldIndex @see [[io.scalacraft.core.marshalling.Marshaller#contextFieldIndex contextFieldIndex]]
   */
  class ShortMarshaller(val isUnsigned: Boolean = false, val contextFieldIndex: Option[Int] = None) extends Marshaller {
    override def marshal(obj: Any)(implicit outStream: DataOutputStream): Unit = obj match {
      case i: Int =>
        outStream.write((i >> 8) & 0xFF)
        outStream.write(i & 0xFF)
    }

    override def internalUnmarshal()(implicit context: Context, inStream: DataInputStream): Any = {
      val s = (inStream.readIfIsAvailable() << 8) | inStream.readIfIsAvailable()
      context.addField(s)
      if (isUnsigned) s else s.toShort.toInt
    }
  }

  /**
   * A marshaller for int type.
   *
   * @param contextFieldIndex @see [[io.scalacraft.core.marshalling.Marshaller#contextFieldIndex contextFieldIndex]]
   */
  class IntMarshaller(val contextFieldIndex: Option[Int] = None) extends Marshaller {
    override def marshal(obj: Any)(implicit outStream: DataOutputStream): Unit = obj match {
      case i: Int =>
        outStream.write((i >> 24) & 0xFF)
        outStream.write((i >> 16) & 0xFF)
        outStream.write((i >> 8) & 0xFF)
        outStream.write(i & 0xFF)
    }

    override def internalUnmarshal()(implicit context: Context, inStream: DataInputStream): Any = {
      val i = (inStream.readIfIsAvailable() << 24) | (inStream.readIfIsAvailable() << 16) |
        (inStream.readIfIsAvailable() << 8) | inStream.readIfIsAvailable()
      context.addField(i)
      i
    }
  }

  /**
   * A marshaller for boolean type.
   *
   * @param contextFieldIndex @see [[io.scalacraft.core.marshalling.Marshaller#contextFieldIndex contextFieldIndex]]
   */
  class LongMarshaller(val contextFieldIndex: Option[Int] = None) extends Marshaller {
    override def marshal(obj: Any)(implicit outStream: DataOutputStream): Unit = obj match {
      case l: Long =>
        outStream.write(((l >> 56) & 0xFF).toInt)
        outStream.write(((l >> 48) & 0xFF).toInt)
        outStream.write(((l >> 40) & 0xFF).toInt)
        outStream.write(((l >> 32) & 0xFF).toInt)
        outStream.write(((l >> 24) & 0xFF).toInt)
        outStream.write(((l >> 16) & 0xFF).toInt)
        outStream.write(((l >> 8) & 0xFF).toInt)
        outStream.write((l & 0xFF).toInt)
    }

    override def internalUnmarshal()(implicit context: Context, inStream: DataInputStream): Any = {
      val l = (inStream.readIfIsAvailable().toLong << 56) | (inStream.readIfIsAvailable().toLong << 48) |
        (inStream.readIfIsAvailable().toLong << 40) | (inStream.readIfIsAvailable().toLong << 32) |
        (inStream.readIfIsAvailable().toLong << 24) | (inStream.readIfIsAvailable().toLong << 16) |
        (inStream.readIfIsAvailable().toLong << 8) | inStream.readIfIsAvailable().toLong
      context.addField(l)
      l
    }
  }

  /**
   * A marshaller for float type.
   *
   * @param contextFieldIndex @see [[io.scalacraft.core.marshalling.Marshaller#contextFieldIndex contextFieldIndex]]
   */
  class FloatMarshaller(val contextFieldIndex: Option[Int] = None) extends Marshaller {
    override def marshal(obj: Any)(implicit outStream: DataOutputStream): Unit = obj match {
      case f: Float =>
        val bytes = java.lang.Float.floatToIntBits(f)
        new IntMarshaller().marshal(bytes)
    }

    override def internalUnmarshal()(implicit context: Context, inStream: DataInputStream): Any = {
      val f = java.lang.Float.intBitsToFloat(new IntMarshaller().unmarshal.asInstanceOf[Int])
      context.addField(f)
      f
    }
  }

  /**
   * A marshaller for double type.
   *
   * @param contextFieldIndex @see [[io.scalacraft.core.marshalling.Marshaller#contextFieldIndex contextFieldIndex]]
   */
  class DoubleMarshaller(val contextFieldIndex: Option[Int] = None) extends Marshaller {
    override def marshal(obj: Any)(implicit outStream: DataOutputStream): Unit = obj match {
      case d: Double =>
        val bytes = java.lang.Double.doubleToLongBits(d)
        new LongMarshaller().marshal(bytes)
    }

    override def internalUnmarshal()(implicit context: Context, inStream: DataInputStream): Any = {
      val d = java.lang.Double.longBitsToDouble(new LongMarshaller().unmarshal()(Context.trash, inStream).asInstanceOf[Long])
      context.addField(d)
      d
    }

  }

  /**
   * A marshaller for variable integer type.
   *
   * @param contextFieldIndex @see [[io.scalacraft.core.marshalling.Marshaller#contextFieldIndex contextFieldIndex]]
   */
  class VarIntMarshaller(val contextFieldIndex: Option[Int] = None) extends Marshaller {
    override def marshal(obj: Any)(implicit outStream: DataOutputStream): Unit = obj match {
      case i: Int => Helpers.writeVarInt(i, outStream)
    }

    override def internalUnmarshal()(implicit context: Context, inStream: DataInputStream): Any = {
      val value = Helpers.readVarInt(inStream).value
      context.addField(value)
      value
    }
  }

  /**
   * A marshaller for variable long type.
   *
   * @param contextFieldIndex @see [[io.scalacraft.core.marshalling.Marshaller#contextFieldIndex contextFieldIndex]]
   */
  class VarLongMarshaller(val contextFieldIndex: Option[Int] = None) extends Marshaller {
    override def marshal(obj: Any)(implicit outStream: DataOutputStream): Unit = obj match {
      case i: Long =>
        var value = i
        do {
          var temp = value & 0x7f
          value = value >>> 7
          if (value != 0) {
            temp |= 0x80
          }
          outStream.write(temp.toInt)
        } while (value != 0)
    }

    override def internalUnmarshal()(implicit context: Context, inStream: DataInputStream): Any = {
      var numRead = 0
      var result = 0
      var read = 0
      do {
        read = inStream.read()
        result |= ((read & 0x7f) << (7 * numRead))
        numRead += 1
        if (numRead > 10) {
          throw new IllegalArgumentException("VarLong is too big")
        }
      } while ((read & 0x80) != 0)

      context.addField(result)
      result
    }
  }

  /**
   * A marshaller for [[io.scalacraft.packets.DataTypes.Position Position]].
   *
   * @param contextFieldIndex @see [[io.scalacraft.core.marshalling.Marshaller#contextFieldIndex contextFieldIndex]]
   */
  class PositionMarshaller(val contextFieldIndex: Option[Int] = None) extends Marshaller {
    override def marshal(obj: Any)(implicit outStream: DataOutputStream): Unit = obj match {
      case Position(x, y, z) =>
        val position: Long = ((x.toLong & 0x3FFFFFF) << 38) | ((y.toLong & 0xFFF) << 26) | (z.toLong & 0x3FFFFFF)
        new LongMarshaller().marshal(position)
    }

    override def internalUnmarshal()(implicit context: Context, inStream: DataInputStream): Any = {
      val longPosition = new LongMarshaller().unmarshal()(Context.trash, inStream).asInstanceOf[Long]
      val x = (longPosition >> 38).toInt
      val y = ((longPosition >> 26) & 0xFFF).toInt
      val z = (longPosition << 38 >> 38).toInt
      val p = Position(x, y, z)
      context.addField(p)
      p
    }
  }

  /**
   * A marshaller for [[io.scalacraft.packets.DataTypes.Angle Angle]].
   *
   * @param contextFieldIndex @see [[io.scalacraft.core.marshalling.Marshaller#contextFieldIndex contextFieldIndex]]
   */
  class AngleMarshaller(val contextFieldIndex: Option[Int] = None) extends Marshaller {
    override def marshal(obj: Any)(implicit outStream: DataOutputStream): Unit = obj match {
      case Angle(value) => outStream.write(value & 0xFF)
    }

    override def internalUnmarshal()(implicit context: Context, inStream: DataInputStream): Any = {
      val angle = Angle(inStream.readIfIsAvailable())
      context.addField(angle)
      angle
    }
  }

  /**
   * A marshaller for string type.
   *
   * @param maxLength the max length that the string can be
   * @param contextFieldIndex @see [[io.scalacraft.core.marshalling.Marshaller#contextFieldIndex contextFieldIndex]]
   */
  class StringMarshaller(maxLength: Int, val contextFieldIndex: Option[Int] = None) extends Marshaller {
    override def marshal(obj: Any)(implicit outStream: DataOutputStream): Unit = obj match {
      case s: String =>
        checkSize(s)
        val buffer = s.getBytes(StandardCharsets.UTF_8)
        val length = buffer.length
        new VarIntMarshaller().marshal(length)

        for (i <- 0 until length) {
          outStream.write(buffer(i))
        }
    }

    override def internalUnmarshal()(implicit context: Context, inStream: DataInputStream): Any = {
      val length = new VarIntMarshaller().unmarshal()(Context.trash, inStream).asInstanceOf[Int]
      val buffer = new Array[Byte](length)
      for (i <- 0 until length) {
        buffer(i) = inStream.readIfIsAvailable().toByte
      }

      val str = new String(buffer, StandardCharsets.UTF_8)
      checkSize(str)
      context.addField(str)
      str
    }

    private def checkSize(str: String): Unit = {
      if (str.length > maxLength) {
        throw new IllegalArgumentException(s"String too big (${str.length} > $maxLength)")
      }
    }
  }

  /**
   * A marshaller for [[java.util.UUID UUID]].
   *
   * @param contextFieldIndex @see [[io.scalacraft.core.marshalling.Marshaller#contextFieldIndex contextFieldIndex]]
   */
  class UUIDMarshaller(val contextFieldIndex: Option[Int] = None) extends Marshaller {
    override def marshal(obj: Any)(implicit outStream: DataOutputStream): Unit = obj match {
      case u: UUID =>
        val longMarshaller = new LongMarshaller()
        longMarshaller.marshal(u.getMostSignificantBits)
        longMarshaller.marshal(u.getLeastSignificantBits)
    }

    override def internalUnmarshal()(implicit context: Context, inStream: DataInputStream): Any = {
      val longMarshaller = new LongMarshaller()
      val leastSignificantBits = longMarshaller.unmarshal()(Context.trash, inStream).asInstanceOf[Long]
      val mostSignificantBits = longMarshaller.unmarshal()(Context.trash, inStream).asInstanceOf[Long]

      val u = new UUID(leastSignificantBits, mostSignificantBits)
      context.addField(u)
      u
    }
  }

  /**
   * A marshaller for optional types.
   *
   * @param paramMarshaller the marshaller of type of the parameter of optional
   * @param conditionMarshaller the marshaller of the condition that indicate if the optional is present
   * @param contextFieldIndex @see [[io.scalacraft.core.marshalling.Marshaller#contextFieldIndex contextFieldIndex]]
   */
  class OptionalMarshaller(paramMarshaller: Marshaller, conditionMarshaller: Option[Marshaller] = None,
                           val contextFieldIndex: Option[Int] = None) extends Marshaller {
    override def marshal(obj: Any)(implicit outStream: DataOutputStream): Unit = obj match {
      case Some(value) if conditionMarshaller.isEmpty =>
        new BooleanMarshaller().marshal(true)
        paramMarshaller.marshal(value)
      case Some(value) =>
        paramMarshaller.marshal(value)
      case None => new BooleanMarshaller().marshal(false)
    }

    override def internalUnmarshal()(implicit context: Context, inStream: DataInputStream): Any = {
      if ((conditionMarshaller.isEmpty && new BooleanMarshaller().unmarshal()(Context.trash, inStream).asInstanceOf[Boolean]) ||
        (conditionMarshaller.isDefined && checkIfTrue(conditionMarshaller.get.unmarshal()(Context.trash, inStream)))) {
        val content = paramMarshaller.unmarshal()
        Some(content)
      } else {
        context.addField(None)
        None
      }
    }

    def checkIfTrue(obj: Any): Boolean = obj match {
      case b: Boolean => b
      case i: Int if i > 0 => true
      case l: Long if l > 0 => true
      case _ => false
    }
  }

  /**
   * A marshaller for array of bytes.
   *
   * @param lengthMarshaller the marshaller for the length of the array
   * @param contextFieldIndex @see [[io.scalacraft.core.marshalling.Marshaller#contextFieldIndex contextFieldIndex]]
   */
  class ByteArrayMarshaller(lengthMarshaller: Option[Marshaller],
                            val contextFieldIndex: Option[Int] = None) extends Marshaller {
    override def marshal(obj: Any)(implicit outStream: DataOutputStream): Unit = obj match {
      case byteArray: Array[Byte] =>
        if (lengthMarshaller.isDefined) {
          lengthMarshaller.get.marshal(byteArray.length)
        }
        outStream.write(byteArray)
    }

    override def internalUnmarshal()(implicit context: Context, inStream: DataInputStream): Any = {
      val byteArray = if (lengthMarshaller.isDefined) {
        val byteArray = new Array[Byte](readLength(lengthMarshaller.get))
        inStream.read(byteArray)

        byteArray
      } else {
        var b: Byte = 0
        val buffer = mutable.ArrayBuffer[Byte]()
        while ( {
          b = inStream.read().toByte; b
        } >= 0) {
          buffer.append(b)
        }

        buffer.toArray
      }

      context.addField(byteArray)
      byteArray
    }
  }

  private def readLength(lengthMarshaller: Marshaller)(implicit inStream: DataInputStream): Int =
    lengthMarshaller.unmarshal()(Context.trash, inStream) match {
      case b: Byte => b
      case s: Short => s
      case i: Int => i
    }

  /**
   * A marshaller for lists.
   *
   * @param paramMarshaller the marshaller of the type of the parameter of optional
   * @param lengthMarshaller the marshaller for the length of the list
   * @param contextFieldIndex @see [[io.scalacraft.core.marshalling.Marshaller#contextFieldIndex contextFieldIndex]]
   */
  class ListMarshaller(paramMarshaller: Marshaller, lengthMarshaller: Option[Marshaller],
                       val contextFieldIndex: Option[Int] = None) extends Marshaller {
    override def marshal(obj: Any)(implicit outStream: DataOutputStream): Unit = obj match {
      case list: List[_] =>
        if (lengthMarshaller.isDefined) {
          lengthMarshaller.get.marshal(list.length)
        }
        list.foreach {
          paramMarshaller.marshal
        }
    }

    override def internalUnmarshal()(implicit context: Context, inStream: DataInputStream): Any = {
      val trash = Context.trash

      val list = if (lengthMarshaller.isDefined) {
        val length = readLength(lengthMarshaller.get)
        val array = new Array[Any](length)

        for (i <- 0 until length) {
          array(i) = paramMarshaller.unmarshal()(trash, inStream)
        }

        array.toList
      } else {
        var stop = false
        val buffer = mutable.ArrayBuffer[Any]()
        while (!stop) {
          try buffer.append(paramMarshaller.unmarshal()(trash, inStream))
          catch {
            case _: EOFException => stop = true
          }
        }

        buffer.toList
      }

      context.addField(list)
      list
    }
  }

  /**
   * A marshaller for structures.
   *
   * @param fieldsMarshaller a list of marshallers, one for each field
   * @param constructorMirror the constructor mirror used to create an instance of the structure via reflection
   * @param contextFieldIndex @see [[io.scalacraft.core.marshalling.Marshaller#contextFieldIndex contextFieldIndex]]
   */
  class StructureMarshaller(fieldsMarshaller: List[Marshaller], constructorMirror: MethodMirror,
                            val contextFieldIndex: Option[Int] = None) extends Marshaller {
    override def marshal(obj: Any)(implicit outStream: DataOutputStream): Unit = obj match {
      case str: Product =>
        str.productIterator.zip(fieldsMarshaller.toIterator) foreach {
          case (obj, marshaller) => marshaller.marshal(obj)
        }
    }

    override def internalUnmarshal()(implicit context: Context, inStream: DataInputStream): Any = {
      val newContext = Context.create
      val fields = fieldsMarshaller map {
        _.unmarshal()(context, inStream)
      }
      context.addField(newContext)
      constructorMirror(fields: _*)
    }
  }

  /**
   * A marshaller for switch types.
   *
   * @param keyMarshaller the marshaller of the key param of the switch
   * @param valuesMarshaller a map where keys are all possible keys of the switch and values are the relative marshallers
   * @param valuesTypes a map where keys are all possible [[scala.reflect.api.JavaUniverse#RuntimeClass RuntimeClass]]
   *                    of the keys of the switch and values are the key of the switch
   * @param takeKeyFromContext true if the key of the switch need to be taken from context
   */
  class SwitchMarshaller(keyMarshaller: Marshaller,
                         valuesMarshaller: Map[Any, Marshaller],
                         valuesTypes: Map[RuntimeClass, Any],
                         takeKeyFromContext: Boolean = false) extends Marshaller {
    val contextFieldIndex: Option[Int] = None

    override def marshal(obj: Any)(implicit outStream: DataOutputStream): Unit = {
      obj match {
        case list: List[_] if list.nonEmpty => marshalClass(valuesTypes(list.head.getClass), obj)
        case Some(value) => marshalClass(valuesTypes(value.getClass), obj)
        case None | _: List[_] => marshalClass(valuesTypes.head._2, obj)
        case obj => marshalClass(valuesTypes(obj.getClass), obj)
      }
    }

    override def internalUnmarshal()(implicit context: Context, inStream: DataInputStream): Any = {
      val key = if (takeKeyFromContext) {
        keyMarshaller.unmarshal()
      } else {
        keyMarshaller.unmarshal()(Context.trash, inStream)
      }
      valuesMarshaller(key).unmarshal()
    }

    private def marshalClass(key: Any, obj: Any)(implicit outStream: DataOutputStream): Unit = {
      if (!takeKeyFromContext) {
        keyMarshaller.marshal(key)
      }
      valuesMarshaller(key).marshal(obj)
    }
  }

  /**
   * A marshaller for enum types.
   *
   * @param valueMarshaller the marshaller for the type of the enum
   * @param valuesInstances a map where keys are the enum keys and the values are the enum instances
   * @param takeKeyFromContext true if the key of the switch need to be taken from context
   */
  class EnumMarshaller(valueMarshaller: Marshaller,
                       valuesInstances: Map[Any, Any],
                       takeKeyFromContext: Boolean = false) extends Marshaller {
    val contextFieldIndex: Option[Int] = None

    override def marshal(obj: Any)(implicit outStream: DataOutputStream): Unit = {
      val value = valuesInstances collectFirst {
        case (value, instance) if instance == obj => value
      }
      valueMarshaller.marshal(value.get)
    }

    override def internalUnmarshal()(implicit context: Context, inStream: DataInputStream): Any = {
      val key = if (takeKeyFromContext) {
        valueMarshaller.unmarshal()
      } else {
        valueMarshaller.unmarshal()(Context.trash, inStream)
      }
      val content = valuesInstances(key)
      context.addField(key)
      content
    }
  }

  /**
   * A marshaller for Ntb types.
   *
   * @param contextFieldIndex @see [[io.scalacraft.core.marshalling.Marshaller#contextFieldIndex contextFieldIndex]]
   */
  class NbtMarshaller(val contextFieldIndex: Option[Int] = None) extends Marshaller {
    override def marshal(obj: Any)(implicit outStream: DataOutputStream): Unit = obj match {
      case tag: Tag[_] => tag.serialize(outStream, Tag.DEFAULT_MAX_DEPTH)
    }

    override def internalUnmarshal()(implicit context: Context, inStream: DataInputStream): Any = {
      val content = Tag.deserialize(inStream, Tag.DEFAULT_MAX_DEPTH)
      context.addField(content)
      content
    }
  }

  /**
   * A marshaller for entities.
   *
   * @param constructorMirrors a map where keys are all possible types of an entity and values are the relative mirror
   *                           used to create the instance with reflection   *
   * @param typeMarshaller the marshaller of the entity type
   * @param typesMarshallers a map where keys are all possible types of fields of an entity and values are the relative
   *                         marshallers
   * @param customType if defined use this value for entity type instead of read it with `typeMarshaller`
   * @param contextFieldIndex @see [[io.scalacraft.core.marshalling.Marshaller#contextFieldIndex contextFieldIndex]]
   */
  class EntityMarshaller(constructorMirrors: Map[Int, MethodMirror], typeMarshaller: Marshaller,
                         typesMarshallers: Seq[Marshaller], customType: Option[Int] = None,
                         val contextFieldIndex: Option[Int] = None) extends Marshaller {
    val byteMarshaller = new ByteMarshaller(true)
    val varIntMarshaller = new VarIntMarshaller()

    override def marshal(obj: Any)(implicit outStream: DataOutputStream): Unit = obj match {
      case obj: EntityMetadata =>
        for (index <- obj.values.indices) {
          byteMarshaller.marshal(index)
          varIntMarshaller.marshal(obj.indexes(index))
          typesMarshallers(obj.indexes(index)).marshal(obj.values(index))
        }
        byteMarshaller.marshal(0xff) //end of metadata
    }

    def internalUnmarshal()(implicit context: Context, inStream: DataInputStream): Any = {
      val trashContext = Context.trash
      var fieldSeq: List[Any] = List()
      var index = 0x00
      var indexToBeReaded = 0
      while ( {
        index = byteMarshaller.unmarshal()(trashContext, inStream).asInstanceOf[Int].toByte
        index
      } != -1) {
        if (index != indexToBeReaded) {
          fieldSeq :+= null
        } else {
          val typeIndex = varIntMarshaller.unmarshal()(trashContext, inStream).asInstanceOf[Int]
          fieldSeq :+= typesMarshallers(typeIndex).unmarshal()(trashContext, inStream)
        }
        indexToBeReaded += 1
      }
      val typeIndex = customType.getOrElse(typeMarshaller.unmarshal().asInstanceOf[Int])
      val obj = constructorMirrors(typeIndex)().asInstanceOf[EntityMetadata]
      obj.setValues(fieldSeq)

      context.addField(obj)
      obj
    }
  }

}
