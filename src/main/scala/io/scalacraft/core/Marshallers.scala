package io.scalacraft.core

import java.io.{BufferedInputStream, BufferedOutputStream}
import java.nio.charset.StandardCharsets
import java.util.UUID

import io.scalacraft.core.DataTypes.{Nbt, Position}
import io.scalacraft.core.nbt.Io

import scala.language.postfixOps
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

object Marshallers {

  object BooleanMarshaller extends Marshaller {
    override def marshal(obj: Any)(implicit outStream: BufferedOutputStream): Unit = obj match {
      case b: Boolean => outStream.write(if (b) 0x1 else 0x0)
    }

    override def unmarshal()(implicit inStream: BufferedInputStream): Any =
      inStream.read() == 0x1
  }

  object ByteMarshaller extends Marshaller {
    override def marshal(obj: Any)(implicit outStream: BufferedOutputStream): Unit = obj match {
      case i: Int => outStream.write(i & 0xFF)
    }

    override def unmarshal()(implicit inStream: BufferedInputStream): Any =
      inStream.read()
  }

  object ShortMarshaller extends Marshaller {
    override def marshal(obj: Any)(implicit outStream: BufferedOutputStream): Unit = obj match {
      case i: Int =>
        outStream.write((i >> 8) & 0xFF)
        outStream.write(i & 0xFF)
    }

    override def unmarshal()(implicit inStream: BufferedInputStream): Any =
      (inStream.read() << 8) | inStream.read()
  }

  object IntMarshaller extends Marshaller {
    override def marshal(obj: Any)(implicit outStream: BufferedOutputStream): Unit = obj match {
      case i: Int =>
        outStream.write((i >> 24) & 0xFF)
        outStream.write((i >> 16) & 0xFF)
        outStream.write((i >> 8) & 0xFF)
        outStream.write(i & 0xFF)
    }

    override def unmarshal()(implicit inStream: BufferedInputStream): Any =
      (inStream.read() << 24) | (inStream.read() << 16) | (inStream.read() << 8) | inStream.read()
  }

  object LongMarshaller extends Marshaller {
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

    override def unmarshal()(implicit inStream: BufferedInputStream): Any =
      (inStream.read().toLong << 56) |(inStream.read().toLong << 48) | (inStream.read().toLong << 40) |
        (inStream.read().toLong << 32) | (inStream.read() << 24) | (inStream.read() << 16) | (inStream.read() << 8) |
        inStream.read()
  }

  object FloatMarshaller extends Marshaller {
    override def marshal(obj: Any)(implicit outStream: BufferedOutputStream): Unit = obj match {
      case f: Float =>
        val bytes = java.lang.Float.floatToIntBits(f)
        IntMarshaller.marshal(bytes)
    }

    override def unmarshal()(implicit inStream: BufferedInputStream): Any =
      java.lang.Float.intBitsToFloat(IntMarshaller.unmarshal().asInstanceOf[Int])
  }

  object DoubleMarshaller extends Marshaller {
    override def marshal(obj: Any)(implicit outStream: BufferedOutputStream): Unit = obj match {
      case d: Double =>
        val bytes = java.lang.Double.doubleToRawLongBits(d)
        LongMarshaller.marshal(bytes)
    }

    override def unmarshal()(implicit inStream: BufferedInputStream): Any =
      java.lang.Double.doubleToRawLongBits(LongMarshaller.unmarshal().asInstanceOf[Double])

  }

  object VarIntMarshaller extends Marshaller with VarMarshaller {
    override def marshal(obj: Any)(implicit outStream: BufferedOutputStream): Unit = obj match {
      case i: Int => variableValuesMarshaller(i)
    }

    override def unmarshal()(implicit inStream: BufferedInputStream): Any = variableValuesUnmarshaller(5)
  }

  object VarLongMarshaller extends Marshaller with VarMarshaller {
    override def marshal(obj: Any)(implicit outStream: BufferedOutputStream): Unit = obj match {
      case l: Long => variableValuesMarshaller(l)
    }

    override def unmarshal()(implicit inStream: BufferedInputStream): Any = variableValuesUnmarshaller(10)
  }

  object PositionMarshaller  extends Marshaller {
    override def marshal(obj: Any)(implicit outStream: BufferedOutputStream): Unit = obj match{
      case Position(x,y,z) =>
        val position:Long = ((x.toLong & 0x3FFFFFF) << 38) | ((y.toLong & 0xFFF) << 26) | (z.toLong & 0x3FFFFFF)
        LongMarshaller.marshal(position)
    }

    override def unmarshal()(implicit inStream: BufferedInputStream): Any = {
        val longPosition = LongMarshaller.unmarshal().asInstanceOf[Long]
        val x = (longPosition >> 38).toInt
        val y = ((longPosition >> 26) & 0xFFF).toInt
        val z = (longPosition << 38 >> 38).toInt
        Position(x,y,z)
    }
  }

  class StringMarshaller(maxLength: Int) extends Marshaller {
    override def marshal(obj: Any)(implicit outStream: BufferedOutputStream): Unit = obj match {
      case s: String =>
        checkSize(s)
        val buffer = s.getBytes(StandardCharsets.UTF_8)
        val length = buffer.length
        VarIntMarshaller.marshal(length)

        for (i <- 0 until length) {
          outStream.write(buffer(i))
        }
    }

    override def unmarshal()(implicit inStream: BufferedInputStream): Any = {
      val length = VarIntMarshaller.unmarshal().asInstanceOf[Int]
      val buffer = new Array[Byte](length)
      for (i <- 0 until length) {
        buffer(i) = inStream.read().toByte
      }

      val str = new String(buffer, StandardCharsets.UTF_8)
      checkSize(str)
      str
    }

    private def checkSize(str: String): Unit = {
      if (str.length > maxLength) {
        throw new IllegalArgumentException(s"String too big (${str.length} > $maxLength)")
      }
    }
  }

  object UUIDMarshaller extends Marshaller {
    private val UUID_Size = 16

    override def marshal(obj: Any)(implicit outStream: BufferedOutputStream): Unit = obj match {
      case u: UUID =>
        LongMarshaller.marshal(u.getMostSignificantBits)
        LongMarshaller.marshal(u.getLeastSignificantBits)
    }

    override def unmarshal()(implicit inStream: BufferedInputStream): Any = {
      val leastSignificantBits = LongMarshaller.unmarshal().asInstanceOf[Long]
      val mostSignificantBits = LongMarshaller.unmarshal().asInstanceOf[Long]

      new UUID(leastSignificantBits, mostSignificantBits)
    }
  }

  class OptionalMarshaller(paramMarshaller: Marshaller) extends Marshaller {
    override def marshal(obj: Any)(implicit outStream: BufferedOutputStream): Unit = obj match {
      case Some(value) =>
        BooleanMarshaller.marshal(true)
        paramMarshaller.marshal(value)
      case None => BooleanMarshaller.marshal(false)
    }

    override def unmarshal()(implicit inStream: BufferedInputStream): Any = {
      if (BooleanMarshaller.unmarshal().asInstanceOf[Boolean]) {
        Some(paramMarshaller.unmarshal())
      } else {
        None
      }
    }
  }

  class ArrayMarshaller(paramMarshaller: Marshaller, lengthMarshaller: Option[Marshaller], runtimeClass: RuntimeClass)
    extends Marshaller {
    override def marshal(obj: Any)(implicit outStream: BufferedOutputStream): Unit = obj match {
      case array: Array[Any] =>
        if(lengthMarshaller.isDefined) lengthMarshaller.get.marshal(array.length)
        for (elem <- array) {
          paramMarshaller.marshal(elem)
        }
    }

    override def unmarshal()(implicit inStream: BufferedInputStream): Any = {
      val length = if (lengthMarshaller.isDefined) {
        lengthMarshaller.get.unmarshal().asInstanceOf[Int]
      } else {
        inStream.available()
      }
      val array = ClassTag(runtimeClass).newArray(length).asInstanceOf[Array[Any]]
      for (i <- 0 until length) {
        array(i) = paramMarshaller.unmarshal()
      }
      array
    }
  }

  class StructureMarshaller(fieldsMarshaller: List[Marshaller], constructorMirror: MethodMirror) extends Marshaller {
    override def marshal(obj: Any)(implicit outStream: BufferedOutputStream): Unit = obj match {
      case str: Structure =>
        str.productIterator.zip(fieldsMarshaller.toIterator) foreach {
          case (obj, marshaller) => marshaller.marshal(obj)
        }
    }

    override def unmarshal()(implicit inStream: BufferedInputStream): Any = {
      val fields = fieldsMarshaller map { _.unmarshal() }
      constructorMirror(fields :_*)
    }
  }

  class SwitchMarshaller(keyMarshaller: Marshaller,
                         valuesMarshaller: Map[Any, Marshaller],
                         valuesTypes: Map[RuntimeClass, Any]) extends Marshaller {
    override def marshal(obj: Any)(implicit outStream: BufferedOutputStream): Unit = {
      val key = valuesTypes(obj.getClass)
      keyMarshaller.marshal(key)
      valuesMarshaller(key).marshal(obj)
    }

    override def unmarshal()(implicit inStream: BufferedInputStream): Any = {
      val key = keyMarshaller.unmarshal()
      valuesMarshaller(key).unmarshal()
    }
  }
  trait VarMarshaller {
     protected def variableValuesUnmarshaller(upperBound: Integer)(implicit inStream: BufferedInputStream): Int = {
      var numRead = 0
      var result = 0
      var read = 0
      do {
        read = inStream.read()
        result |= ((read & 0x7f) << (7 * numRead))
        numRead += 1
        if (numRead > upperBound) {
          throw new IllegalArgumentException("Var is too big")
        }
      } while ((read & 0x80) != 0)
      result
    }
    protected def variableValuesMarshaller(obj: Long)(implicit outStream: BufferedOutputStream): Unit = {
      var value = obj
      do {
        var temp = value & 0x7f
        value = value >>> 7
        if (value != 0) {
          temp |= 0x80
        }
        outStream.write(temp.toInt)
      } while (value != 0)
    }
  }

  object NbtMarshaller extends Marshaller {
    override def marshal(obj: Any)(implicit outStream: BufferedOutputStream): Unit = obj match {
      case Nbt(name, compound) => Io.writeNBT(outStream)((name, compound))
    }

    override def unmarshal()(implicit inStream: BufferedInputStream): Any = Io.readNBT(inStream) match {
      case (name, compoundTag) => Nbt(name, compoundTag)
    }
  }
}
