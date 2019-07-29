package io.scalacraft.core

import java.io.{BufferedInputStream, BufferedOutputStream}
import java.nio.charset.StandardCharsets
import java.util.UUID
import scala.reflect.runtime.universe.MethodMirror

object Marshallers {

  object BooleanMarshaller extends Marshaller[Boolean] {
    override def marshal(obj: Boolean)(implicit outStream: BufferedOutputStream): Unit = {
      outStream.write(if (obj) 0x1 else 0x0)
    }

    override def unmarshal()(implicit inStream: BufferedInputStream): Boolean = {
      inStream.read() == 0x1
    }
  }

  object ByteMarshaller extends Marshaller[Int] {
    override def marshal(obj: Int)(implicit outStream: BufferedOutputStream): Unit = {
      outStream.write(obj & 0xFF)
    }

    override def unmarshal()(implicit inStream: BufferedInputStream): Int = {
      inStream.read()
    }
  }

  object ShortMarshaller extends Marshaller[Int] {
    override def marshal(obj: Int)(implicit outStream: BufferedOutputStream): Unit = {
      outStream.write((obj >> 8) & 0xFF)
      outStream.write(obj & 0xFF)
    }

    override def unmarshal()(implicit inStream: BufferedInputStream): Int = {
      (inStream.read() << 8) | inStream.read()
    }
  }

  object IntMarshaller extends Marshaller[Int] {
    override def marshal(obj: Int)(implicit outStream: BufferedOutputStream): Unit = {
      outStream.write((obj >> 24) & 0xFF)
      outStream.write((obj >> 16) & 0xFF)
      outStream.write((obj >> 8) & 0xFF)
      outStream.write(obj & 0xFF)
    }

    override def unmarshal()(implicit inStream: BufferedInputStream): Int = {
      (inStream.read() << 24) | (inStream.read() << 16) | (inStream.read() << 8) | inStream.read()
    }
  }

  object LongMarshaller extends Marshaller[Long] {
    override def marshal(obj: Long)(implicit outStream: BufferedOutputStream): Unit = {
      outStream.write(((obj >> 56) & 0xFF).toInt)
      outStream.write(((obj >> 48) & 0xFF).toInt)
      outStream.write(((obj >> 40) & 0xFF).toInt)
      outStream.write(((obj >> 32) & 0xFF).toInt)
      outStream.write(((obj >> 24) & 0xFF).toInt)
      outStream.write(((obj >> 16) & 0xFF).toInt)
      outStream.write(((obj >> 8) & 0xFF).toInt)
      outStream.write((obj & 0xFF).toInt)
    }

    override def unmarshal()(implicit inStream: BufferedInputStream): Long = {
      (inStream.read() << 56) |(inStream.read() << 48) | (inStream.read() << 40) | (inStream.read() << 32) |
        (inStream.read() << 24) | (inStream.read() << 16) | (inStream.read() << 8) | inStream.read()
    }
  }

  object VarIntMarshaller extends Marshaller[Int] {
    override def marshal(obj: Int)(implicit outStream: BufferedOutputStream): Unit = {
      var value = obj
      do {
        var temp = value & 0x7f
        value = value >>> 7
        if (value != 0) {
          temp |= 0x80
        }
        outStream.write(temp)
      } while (value != 0)
    }

    override def unmarshal()(implicit inStream: BufferedInputStream): Int = {
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

      result
    }
  }

  class StringMarshaller(maxLength: Int) extends Marshaller[String] {
    override def marshal(obj: String)(implicit outStream: BufferedOutputStream): Unit = {
      if (obj.length > maxLength) {
        throw new IllegalArgumentException(s"String too big (${obj.length} > $maxLength)")
      }

      val buffer = obj.getBytes(StandardCharsets.UTF_8)
      val length = buffer.length
      VarIntMarshaller.marshal(length)

      for (i <- 0 until length) {
        outStream.write(buffer(i))
      }
    }

    override def unmarshal()(implicit inStream: BufferedInputStream): String = {
      val length = VarIntMarshaller.unmarshal()
      val buffer = new Array[Byte](length)
      for (i <- 0 until length) {
        buffer(i) = inStream.read().toByte
      }

      val str = new String(buffer, StandardCharsets.UTF_8)
      if (str.length > maxLength) {
        throw new IllegalArgumentException(s"String too big (${str.length} > $maxLength)")
      }
      str
    }
  }

  object UUIDMarshaller extends Marshaller[UUID] {
    private val UUID_Size = 16

    override def marshal(obj: UUID)(implicit outStream: BufferedOutputStream): Unit = {
      LongMarshaller.marshal(obj.getMostSignificantBits)
      LongMarshaller.marshal(obj.getLeastSignificantBits)
    }

    override def unmarshal()(implicit inStream: BufferedInputStream): UUID = {
      val buffer = new Array[Byte](UUID_Size)
      for (i <- 0 until UUID_Size) {
        buffer(i) = inStream.read().toByte
      }

      UUID.nameUUIDFromBytes(buffer)
    }
  }

  class OptionalMarshaller[T](paramMarshaller: Marshaller[T]) extends Marshaller[Option[T]] {
    override def marshal(obj: Option[T])(implicit outStream: BufferedOutputStream): Unit = {
      BooleanMarshaller.marshal(obj.isDefined)
      if (obj.isDefined) {
        paramMarshaller.marshal(obj.get)
      }
    }

    override def unmarshal()(implicit inStream: BufferedInputStream): Option[T] = {
      if (BooleanMarshaller.unmarshal()) {
        Some(paramMarshaller.unmarshal())
      } else {
        None
      }
    }
  }

  class ArrayMarshaller[T](paramMarshaller: Marshaller[T], lengthMarshaller: Marshaller[Int]) extends Marshaller[Array[T]] {
    override def marshal(obj: Array[T])(implicit outStream: BufferedOutputStream): Unit = {
      lengthMarshaller.marshal(obj.length)
      for (elem <- obj) {
        paramMarshaller.marshal(elem)
      }
    }

    override def unmarshal()(implicit inStream: BufferedInputStream): Array[T] = {
      val length = lengthMarshaller.unmarshal()
      val array: Array[T] = new Array(length)
      for (i <- 0 until length) {
        array(i) = paramMarshaller.unmarshal()
      }
      array
    }
  }

  class StructureMarshaller[T <: Product](fieldsMarshaller: List[Marshaller[Any]], constructorMirror: MethodMirror)
    extends Marshaller[T] {
    override def marshal(obj: T)(implicit outStream: BufferedOutputStream): Unit = {
      obj.productIterator.zip(fieldsMarshaller) foreach { t =>
        t._2.marshal(t._1)
      }
    }

    override def unmarshal()(implicit inStream: BufferedInputStream): T = {
      val fields = fieldsMarshaller map { _.unmarshal() }
      constructorMirror(fields :_*).asInstanceOf[T]
    }
  }

}
