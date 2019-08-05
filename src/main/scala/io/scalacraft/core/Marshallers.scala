package io.scalacraft.core

import java.io.{BufferedInputStream, BufferedOutputStream, EOFException}
import java.nio.charset.StandardCharsets
import java.util.UUID

import io.scalacraft.core.DataTypes.{Nbt, Position}
import io.scalacraft.core.nbt.NbtParser

import scala.collection.mutable
import scala.language.postfixOps
import scala.reflect.runtime.universe._

object Marshallers {

  implicit class RichStream(base: BufferedInputStream) {
    def readIfIsAvailable(): Int = {
      val readedValue = base.read()
      if (readedValue < 0) throw new EOFException else readedValue
    }
  }

  class BooleanMarshaller(val contextFieldIndex: Option[Int] = None) extends Marshaller {
    override def marshal(obj: Any)(implicit outStream: BufferedOutputStream): Unit = obj match {
      case b: Boolean => outStream.write(if (b) 0x1 else 0x0)
    }

    override def internalUnmarshal()(implicit context: Context, inStream: BufferedInputStream): Any = {
      val bool = inStream.readIfIsAvailable() == 0x1
      context.addField(bool)
      bool
    }
  }

  class ByteMarshaller(val isUnsigned: Boolean = false, val contextFieldIndex: Option[Int] = None) extends Marshaller {
    override def marshal(obj: Any)(implicit outStream: BufferedOutputStream): Unit = obj match {
      case i: Int => outStream.write(i & 0xFF)
    }

    override def internalUnmarshal()(implicit context: Context, inStream: BufferedInputStream): Any = {
      val b = inStream.readIfIsAvailable()
      context.addField(b)
      if (isUnsigned) b else b.toByte
    }
  }

  class ShortMarshaller(val isUnsigned: Boolean = false, val contextFieldIndex: Option[Int] = None) extends Marshaller {
    override def marshal(obj: Any)(implicit outStream: BufferedOutputStream): Unit = obj match {
      case i: Int =>
        outStream.write((i >> 8) & 0xFF)
        outStream.write(i & 0xFF)
    }

    override def internalUnmarshal()(implicit context: Context, inStream: BufferedInputStream): Any = {
      val s = (inStream.readIfIsAvailable() << 8) | inStream.readIfIsAvailable()
      context.addField(s)
      if (isUnsigned) s else s.toShort
    }
  }

  class IntMarshaller(val contextFieldIndex: Option[Int] = None) extends Marshaller {
    override def marshal(obj: Any)(implicit outStream: BufferedOutputStream): Unit = obj match {
      case i: Int =>
        outStream.write((i >> 24) & 0xFF)
        outStream.write((i >> 16) & 0xFF)
        outStream.write((i >> 8) & 0xFF)
        outStream.write(i & 0xFF)
    }

    override def internalUnmarshal()(implicit context: Context, inStream: BufferedInputStream): Any = {
      val i = (inStream.readIfIsAvailable() << 24) | (inStream.readIfIsAvailable() << 16) |
        (inStream.readIfIsAvailable() << 8) | inStream.readIfIsAvailable()
      context.addField(i)
      i
    }
  }

  class LongMarshaller(val contextFieldIndex: Option[Int] = None) extends Marshaller {
    override def marshal(obj: Any)(implicit outStream: BufferedOutputStream): Unit = obj match {
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

    override def internalUnmarshal()(implicit context: Context, inStream: BufferedInputStream): Any = {
      val l = (inStream.readIfIsAvailable().toLong << 56) | (inStream.readIfIsAvailable().toLong << 48) |
        (inStream.readIfIsAvailable().toLong << 40) | (inStream.readIfIsAvailable().toLong << 32) |
        (inStream.readIfIsAvailable().toLong << 24) | (inStream.readIfIsAvailable().toLong << 16) |
        (inStream.readIfIsAvailable().toLong << 8) | inStream.readIfIsAvailable().toLong
      context.addField(l)
      l
    }
  }

  class FloatMarshaller(val contextFieldIndex: Option[Int] = None) extends Marshaller {
    override def marshal(obj: Any)(implicit outStream: BufferedOutputStream): Unit = obj match {
      case f: Float =>
        val bytes = java.lang.Float.floatToIntBits(f)
        new IntMarshaller().marshal(bytes)
    }

    override def internalUnmarshal()(implicit context: Context, inStream: BufferedInputStream): Any = {
      val f = java.lang.Float.intBitsToFloat(new IntMarshaller().unmarshal.asInstanceOf[Int])
      context.addField(f)
      f
    }
  }

  class DoubleMarshaller(val contextFieldIndex: Option[Int] = None) extends Marshaller {
    override def marshal(obj: Any)(implicit outStream: BufferedOutputStream): Unit = obj match {
      case d: Double =>
        val bytes = java.lang.Double.doubleToLongBits(d)
        new LongMarshaller().marshal(bytes)
    }

    override def internalUnmarshal()(implicit context: Context, inStream: BufferedInputStream): Any = {
      val d = java.lang.Double.longBitsToDouble(new LongMarshaller().unmarshal().asInstanceOf[Long])
      context.addField(d)
      d
    }

  }

  class VarIntMarshaller(val contextFieldIndex: Option[Int] = None) extends Marshaller {
    override def marshal(obj: Any)(implicit outStream: BufferedOutputStream): Unit = obj match {
      case i: Int =>
        var value = i
        do {
          var temp = value & 0x7f
          value = value >>> 7
          if (value != 0) {
            temp |= 0x80
          }
          outStream.write(temp)
        } while (value != 0)
    }

    override def internalUnmarshal()(implicit context: Context, inStream: BufferedInputStream): Any = {
      var numRead = 0
      var result = 0
      var read = 0
      do {
        read = inStream.read()
        result |= ((read & 0x7f) << (7 * numRead))
        numRead += 1
        if (numRead > 5) {
          throw new IllegalArgumentException("VarInt is too big")
        }
      } while ((read & 0x80) != 0)

      context.addField(result)
      result
    }
  }

  class VarLongMarshaller(val contextFieldIndex: Option[Int] = None) extends Marshaller {
    override def marshal(obj: Any)(implicit outStream: BufferedOutputStream): Unit = obj match {
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

    override def internalUnmarshal()(implicit context: Context, inStream: BufferedInputStream): Any = {
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

  class PositionMarshaller(val contextFieldIndex: Option[Int] = None) extends Marshaller {
    override def marshal(obj: Any)(implicit outStream: BufferedOutputStream): Unit = obj match {
      case Position(x, y, z) =>
        val position: Long = ((x.toLong & 0x3FFFFFF) << 38) | ((y.toLong & 0xFFF) << 26) | (z.toLong & 0x3FFFFFF)
        new LongMarshaller().marshal(position)
    }

    override def internalUnmarshal()(implicit context: Context, inStream: BufferedInputStream): Any = {
      val longPosition = new LongMarshaller().unmarshal().asInstanceOf[Long]
      val x = (longPosition >> 38).toInt
      val y = ((longPosition >> 26) & 0xFFF).toInt
      val z = (longPosition << 38 >> 38).toInt
      val p = Position(x, y, z)
      context.addField(p)
      p
    }
  }

  class StringMarshaller(maxLength: Int, val contextFieldIndex: Option[Int] = None) extends Marshaller {
    override def marshal(obj: Any)(implicit outStream: BufferedOutputStream): Unit = obj match {
      case s: String =>
        checkSize(s)
        val buffer = s.getBytes(StandardCharsets.UTF_8)
        val length = buffer.length
        new VarIntMarshaller().marshal(length)

        for (i <- 0 until length) {
          outStream.write(buffer(i))
        }
    }

    override def internalUnmarshal()(implicit context: Context, inStream: BufferedInputStream): Any = {
      val length = new VarIntMarshaller().unmarshal().asInstanceOf[Int]
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

  class UUIDMarshaller(val contextFieldIndex: Option[Int] = None) extends Marshaller {
    override def marshal(obj: Any)(implicit outStream: BufferedOutputStream): Unit = obj match {
      case u: UUID =>
        val longMarshaller = new LongMarshaller()
        longMarshaller.marshal(u.getMostSignificantBits)
        longMarshaller.marshal(u.getLeastSignificantBits)
    }

    override def internalUnmarshal()(implicit context: Context, inStream: BufferedInputStream): Any = {
      val longMarshaller = new LongMarshaller()
      val leastSignificantBits = longMarshaller.unmarshal().asInstanceOf[Long]
      val mostSignificantBits = longMarshaller.unmarshal().asInstanceOf[Long]

      val u = new UUID(leastSignificantBits, mostSignificantBits)
      context.addField(u)
      u
    }
  }

  class OptionalMarshaller(paramMarshaller: Marshaller, conditionMarshaller: Option[Marshaller] = None,
                           val contextFieldIndex: Option[Int] = None) extends Marshaller {
    override def marshal(obj: Any)(implicit outStream: BufferedOutputStream): Unit = obj match {
      case Some(value) if conditionMarshaller.isEmpty =>
        new BooleanMarshaller().marshal(true)
        paramMarshaller.marshal(value)
      case Some(value) =>
        paramMarshaller.marshal(value)
      case None => new BooleanMarshaller().marshal(false)
    }

    override def internalUnmarshal()(implicit context: Context, inStream: BufferedInputStream): Any = {
      if ((conditionMarshaller.isEmpty && new BooleanMarshaller().unmarshal().asInstanceOf[Boolean]) ||
        (conditionMarshaller.isDefined && checkIfTrue(conditionMarshaller.get.unmarshal()))) {
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

  class ListMarshaller(paramMarshaller: Marshaller, lengthMarshaller: Option[Marshaller],
                       val contextFieldIndex: Option[Int] = None) extends Marshaller {
    override def marshal(obj: Any)(implicit outStream: BufferedOutputStream): Unit = obj match {
      case list: List[_] =>
        if (lengthMarshaller.isDefined) {
          lengthMarshaller.get.marshal(list.length)
        }
        list.foreach {
          paramMarshaller.marshal
        }
    }

    override def internalUnmarshal()(implicit context: Context, inStream: BufferedInputStream): Any = {
      val newContext = Context.create
      context.addField(newContext)
      val buffer = mutable.Buffer[Any]()

      if (lengthMarshaller.isDefined) {
        val length = lengthMarshaller.get.unmarshal().asInstanceOf[Int]

        for (_ <- 0 until length) {
          buffer.append(paramMarshaller.unmarshal()(newContext, inStream))
        }
      } else {
        var stop = false
        while (!stop) {
          try buffer.append(paramMarshaller.unmarshal()(newContext, inStream))
          catch {
            case _: EOFException => stop = true
          }
        }
      }

      buffer.toList
    }
  }

  class StructureMarshaller(fieldsMarshaller: List[Marshaller], constructorMirror: MethodMirror,
                            val contextFieldIndex: Option[Int] = None) extends Marshaller {
    override def marshal(obj: Any)(implicit outStream: BufferedOutputStream): Unit = obj match {
      case str: Product =>
        str.productIterator.zip(fieldsMarshaller.toIterator) foreach {
          case (obj, marshaller) => marshaller.marshal(obj)
        }
    }

    override def internalUnmarshal()(implicit context: Context, inStream: BufferedInputStream): Any = {
      val newContext = Context.create
      val fields = fieldsMarshaller map {
        _.unmarshal()(context, inStream)
      }
      context.addField(newContext)
      constructorMirror(fields: _*)
    }
  }

  class SwitchMarshaller(keyMarshaller: Marshaller,
                         valuesMarshaller: Map[Any, Marshaller],
                         valuesTypes: Map[RuntimeClass, Any],
                         takeKeyFromContext: Boolean = false) extends Marshaller {
    val contextFieldIndex: Option[Int] = None

    override def marshal(obj: Any)(implicit outStream: BufferedOutputStream): Unit = {
      obj match {
        case list: List[_] if list.nonEmpty => marshalClass(valuesTypes(list.head.getClass), obj)
        case Some(value) => marshalClass(valuesTypes(value.getClass), obj)
        case None | _: List[_] => marshalClass(valuesTypes.head._2, obj)
        case obj => marshalClass(valuesTypes(obj.getClass), obj)
      }
    }

    override def internalUnmarshal()(implicit context: Context, inStream: BufferedInputStream): Any = {
      val key = keyMarshaller.unmarshal()
      valuesMarshaller(key).unmarshal()
    }

    private def marshalClass(key: Any, obj: Any)(implicit outStream: BufferedOutputStream): Unit = {
      if (!takeKeyFromContext) {
        keyMarshaller.marshal(key)
      }
      valuesMarshaller(key).marshal(obj)
    }
  }

  class EnumMarshaller(valueMarshaller: Marshaller,
                       valuesInstances: Map[Any, Any]) extends Marshaller {
    val contextFieldIndex: Option[Int] = None

    override def marshal(obj: Any)(implicit outStream: BufferedOutputStream): Unit = {
      val value = valuesInstances collectFirst {
        case (value, instance) if instance == obj => value
      }
      valueMarshaller.marshal(value.get)
    }

    override def internalUnmarshal()(implicit context: Context, inStream: BufferedInputStream): Any = {
      val key = valueMarshaller.unmarshal()
      val content = valuesInstances(key)
      context.addField(key)
      content
    }
  }

  class NbtMarshaller(val contextFieldIndex: Option[Int] = None) extends Marshaller {
    override def marshal(obj: Any)(implicit outStream: BufferedOutputStream): Unit = obj match {
      case Nbt(name, compoundTag) => NbtParser.writeNBT(outStream)((name, compoundTag))
    }

    override def internalUnmarshal()(implicit context: Context, inStream: BufferedInputStream): Any =
      NbtParser.readNBT(inStream) match {
        case (name, compoundTag) =>
          val content = Nbt(name, compoundTag)
          context.addField(content)
          content
      }
  }

  class EntityMarshaller(constructorMirrors: Map[Int, MethodMirror], typeMarshaller: Marshaller, typesMarshallers: Seq[Marshaller], val contextFieldIndex: Option[Int] = None) extends Marshaller {
    val byteMarshaller = new ByteMarshaller(true)
    val varIntMarshaller = new VarIntMarshaller()

    override def marshal(obj: Any)(implicit outStream: BufferedOutputStream): Unit = obj match {
      case obj: EntityMetadata =>
        for (index <- obj.values.indices) {
          byteMarshaller.marshal(index)
          varIntMarshaller.marshal(obj.indexes(index))
          typesMarshallers(obj.indexes(index)).marshal(obj.values(index))
        }
        byteMarshaller.marshal(0xff) //end of metadata
    }

    def internalUnmarshal()(implicit context: Context, inStream: BufferedInputStream): Any = {
      var fieldSeq: List[Any] = List()
      var index = 0x00
      var indexToBeReaded = 0
      while ( {
        index = byteMarshaller.unmarshal().asInstanceOf[Int].toByte; index
      } != -1) {
        if (index != indexToBeReaded) {
          fieldSeq :+= null
        } else {
          val typeIndex = varIntMarshaller.unmarshal().asInstanceOf[Int]
          fieldSeq :+= typesMarshallers(typeIndex).unmarshal()
        }
        indexToBeReaded += 1
      }
      val obj = constructorMirrors(typeMarshaller.unmarshal().asInstanceOf[Int])().asInstanceOf[EntityMetadata]
      obj.setValues(fieldSeq)
      obj
    }
  }

}
