import java.io.{BufferedInputStream, BufferedOutputStream, ByteArrayInputStream, ByteArrayOutputStream}
import java.nio.charset.StandardCharsets
import java.util.UUID

import PlayPackets.{PlayerInfoAction, TestPacket}

import scala.annotation.StaticAnnotation
import scala.language.postfixOps


object Entrypoint extends App {

  val originalPacket = hex2bytes("0100000005")
  val serializedPacket = new ByteArrayOutputStream()
  implicit val inStream: BufferedInputStream = new BufferedInputStream(new ByteArrayInputStream(originalPacket))
  implicit val outStream: BufferedOutputStream = new BufferedOutputStream(serializedPacket)

  val fields = PacketManager.packetFieldsMarshaller(0x0) map { m => m.unmarshal() }
  val jg = PacketManager.instanceConstructors(0x0)(fields: _*).asInstanceOf[TestPacket]

  println(jg)

//  jg.productIterator.zip(PacketManager.packetFieldsMarshaller(0x25)) foreach { tuple =>
//    tuple._2.marshal(tuple._1)
//  }
//
//  inStream.close()
//  outStream.close()
//
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
      case sym if isSymType[Option[_]](sym) =>
        new OptionalMarshaller(sym.info.typeArgs(0))
      case sym if isSymType[Array[_]](sym) =>
        new ArrayMarshaller(sym.info.typeArgs(0))
    }
    list.asInstanceOf[List[Marshaller[Any]]] // todo: bad type conversion :(
  } toMap

  private def isSymType[T: TypeTag](symbol: Symbol): Boolean = symbol.info <:< typeOf[T]
  private def hasAnnotation[T: TypeTag](symbol: Symbol): Boolean = symbol.annotations.exists {_.tree.tpe =:= typeOf[T]}

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


case class VarInt(value: Int) extends AnyVal


object PlayPackets {
  import PacketAnnotations._

  sealed trait PlayerInfoAction {
    val uuid: UUID
  }

  @packetStructure
  case class AddPlayerProperty(name: String,
                               value: String,
                               signature: Option[String])

  case class AddPlayer(
                        uuid: UUID,
                        @string(16) name: String,
                        @array[VarInt] property: Array[AddPlayerProperty],
                        @variableSize gameMode: Int,
                        @variableSize ping: Int,
                        @string(32767) chat: Option[String]
                      ) extends PlayerInfoAction


  @packet(0x30)
  case class PlayerInfo(@switch[VarInt, Class[_]](Map(VarInt(0) -> classOf[AddPlayer])) playerAction: Array[PlayerInfoAction])
    extends Message

  @packet(0x0)
  case class TestPacket(testOption: Option[Int])

}
