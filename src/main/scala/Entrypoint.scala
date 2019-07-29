import java.io.{BufferedInputStream, BufferedOutputStream, ByteArrayInputStream, ByteArrayOutputStream}
import java.nio.charset.StandardCharsets
import java.util.UUID

import scala.annotation.StaticAnnotation
import scala.language.postfixOps


object Entrypoint extends App {

  val originalPacket = hex2bytes("000002a3000000000001140764656661756c7400")
  val serializedPacket = new ByteArrayOutputStream()
  implicit val inStream: BufferedInputStream = new BufferedInputStream(new ByteArrayInputStream(originalPacket))
  implicit val outStream: BufferedOutputStream = new BufferedOutputStream(serializedPacket)

//  val fields = PacketManager.packetFieldsMarshaller(0x25) map { m => m.unmarshal() }
//  val jg = PacketManager.instanceConstructors(0x25)(fields: _*).asInstanceOf[JoinGame]
//
//  jg.productIterator.zip(PacketManager.packetFieldsMarshaller(0x25)) foreach { tuple =>
//    tuple._2.marshal(tuple._1)
//  }

  inStream.close()
  outStream.close()

//  println(jg)
//  assert(originalPacket.toList sameElements serializedPacket.toByteArray)

  //  val a = UUID.nameUUIDFromBytes(hex2bytes("4e8e21a22c7f440e8b05a3c71335336c"))
  //  println(a)

  //  case class Test(value: Int)
  //  val t = Test(42)
  //  val field = t.getClass.getDeclaredField("value")
  //  field.setAccessible(true)
  //  val clazz = Class.forName(classOf[Test].getName)
  //  val ctor = clazz.getConstructor(classOf[Int])
  //  val y = ctor.newInstance(42)
  //  println(y)
  //
  //    val time = System.currentTimeMillis()
  //    for (i <- 0 to 1000000) {
  //
  //      val y = ctor.newInstance(42)
  //
  //    }
  //    println(System.currentTimeMillis() - time)


  def hex2bytes(hex: String): Array[Byte] = {
    hex.replaceAll("[^0-9A-Fa-f]", "")
      .toSeq.sliding(2, 2)
      .map(_.unwrap).toArray
      .map(Integer.parseInt(_, 16).toByte)
  }

  //  PacketManager.packetFieldsMarshaller(0x25)
  //  PacketManager.instanceConstructors(0x25)
  //  val time = System.currentTimeMillis()
  //  for (i <- 0 to 1000000) {
  //    implicit val inStream: BufferedInputStream = new BufferedInputStream(new ByteArrayInputStream(serializedPacket))
  //    val fields = PacketManager.packetFieldsMarshaller(0x25) map { m => m.unmarshal() }
  //    val jg = PacketManager.instanceConstructors(0x25)(fields: _*)
  //  }
  //  println(System.currentTimeMillis() - time)
}

object PacketManager {
  import PacketAnnotations._
  import ValueMarshaller._

  import scala.reflect.runtime.universe._

  private lazy val mirror = runtimeMirror(getClass.getClassLoader)

  private lazy val symbols: Map[Int, ClassSymbol] = typeOf[PlayPackets.type].decls.collect {
    case sym if sym.isClass && sym.asClass.annotations.exists(_.tree.tpe =:= typeOf[packet]) =>
      val annotation = sym.asClass.annotations.find {_.tree.tpe =:= typeOf[packet]} get
      val packetId = annotation.tree.children.tail.collectFirst { case Literal(Constant(value)) => value }.get
      packetId.asInstanceOf[Int] -> sym.asClass
  }.toMap

  lazy val instanceConstructors: Map[Int, MethodMirror] = symbols.mapValues { cs =>
    val cm = mirror.reflectClass(cs)
    val ctor = cs.toType.decl(termNames.CONSTRUCTOR).asMethod
    cm.reflectConstructor(ctor)
  } toMap

  lazy val packetFieldsMarshaller: Map[Int, List[Marshaller[Any]]] = symbols.mapValues { cs =>
    val list: List[Marshaller[_]] = cs.toType.decl(termNames.CONSTRUCTOR).asMethod.paramLists(0) map {
      case sym if isSymType[Int](sym) =>
        if (hasAnnotation[byte](sym) || hasAnnotation[unsignedByte](sym)) {
          ByteMarshaller
        } else if (hasAnnotation[short](sym) || hasAnnotation[unsignedShort](sym)) {
          ShortMarshaller
        } else if (hasAnnotation[variableSize](sym)) {
          VarIntMarshaller
        } else {
          IntMarshaller
        }
      case sym if isSymType[String](sym) =>
        StringMarshaller
      case sym if isSymType[Boolean](sym) =>
        BooleanMarshaller
    }
    list.asInstanceOf[List[Marshaller[Any]]] // todo: bad type conversion :(
  } toMap

  private def isSymType[T: TypeTag](symbol: Symbol): Boolean = symbol.info =:= typeOf[T]
  private def hasAnnotation[T: TypeTag](symbol: Symbol): Boolean = symbol.annotations.exists {_.tree.tpe =:= typeOf[T]}

}

trait Marshaller[T] {
  def marshal(obj: T)(implicit outStream: BufferedOutputStream): Unit
  def unmarshal()(implicit inStream: BufferedInputStream): T
}

object ValueMarshaller {

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

  implicit object IntMarshaller extends Marshaller[Int] {
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
      (inStream.read() << 56) |(inStream.read() << 48) | (inStream.read() << 40) | (inStream.read() << 32) | (inStream.read() << 24) | (inStream.read() << 16) | (inStream.read() << 8) | inStream.read()
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
          throw new Exception("VarInt is too big")
        }
      } while ((read & 0x80) != 0)

      result
    }
  }

  object StringMarshaller extends Marshaller[String] {
    override def marshal(obj: String)(implicit outStream: BufferedOutputStream): Unit = {
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

      new String(buffer, StandardCharsets.UTF_8)
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

  class OptionalMarshaller[T] extends Marshaller[Option[T]] {
    import ValueMarshaller._

    override def marshal(obj: Option[T])(implicit outStream: BufferedOutputStream): Unit = {
      if (obj.isDefined) {
        BooleanMarshaller.marshal(obj = true)
        // marshallers(classOf[T]).marshal(obj.get)
      }
    }

    override def unmarshal()(implicit inStream: BufferedInputStream): Option[T] = {
      if (BooleanMarshaller.unmarshal()) {
        // Some(marshallers(classOf[T]).unmarshal())
        None
      } else {
        None
      }
    }
  }

}

object PacketAnnotations {
  class packet(id: Int) extends StaticAnnotation
  class variableSize extends StaticAnnotation
  class byte extends StaticAnnotation
  class unsignedByte extends StaticAnnotation
  class short extends StaticAnnotation
  class unsignedShort extends StaticAnnotation
  class enum(values: Any*) extends StaticAnnotation
  class string(length: Int) extends StaticAnnotation
  class switch[K,V](values: Map[K, V]) extends StaticAnnotation
  class array[SizeType]() extends StaticAnnotation
  class packetStructure() extends StaticAnnotation
}

trait Message {
  this: Product =>

  override def toString: String = {
    val buffer = new StringBuilder
    buffer append this.productPrefix
    buffer append " { "
    this.productElementNames.zip(this.productIterator) foreach { tuple =>
      buffer append tuple._1
      buffer append " -> "
      buffer append tuple._2
      buffer append " | "
    }
    buffer.delete(buffer.length() - 3, buffer.length() - 1)
    buffer append "}\n"

    buffer toString
  }
}

class VarInt(val value: Int) extends AnyVal



object PlayPackets {
  import PacketAnnotations._



  sealed trait PlayerInfoAction {
    val uuid: UUID
  }

  @packetStructure
  case class AddPlayerProperty(@string(32767) name: String,
                               @string(32767) value: String,
                               @string(32767) signature: Option[String])

  case class AddPlayer(
                        uuid: UUID,
                        @string(16) name: String,
                        @array[VarInt] property: Array[AddPlayerProperty],
                        @variableSize gameMode: Int,
                        @variableSize ping: Int,
                        @string(32767) chat: Option[String]
                      ) extends PlayerInfoAction


  @packet(0x30)
  case class PlayerInfo(@switch[VarInt, PlayerInfoAction](Map(0 -> AddPlayer)) playerAction: Array[PlayerInfoAction])
    extends Message

}
