package io.scalacraft.core

import java.io.{BufferedInputStream, BufferedOutputStream}
import java.util.UUID

import io.scalacraft.core.DataTypes.VarInt
import io.scalacraft.core.Marshallers._
import io.scalacraft.core.PacketAnnotations._

import scala.language.postfixOps
import scala.reflect.runtime.universe._

class PacketManager[T: TypeTag] {

  private lazy val mirror = runtimeMirror(getClass.getClassLoader)

  private lazy val classTypes: List[Type] = typeOf[T].decls.collect {
    case sym if sym.isClass => sym.asClass.toType
  } toList

  private lazy val classConstructors: Map[Type, MethodMirror] = classTypes map { tpe =>
    val cm = mirror.reflectClass(tpe.typeSymbol.asClass)
    val ctor = tpe.decl(termNames.CONSTRUCTOR).asMethod
    tpe -> cm.reflectConstructor(ctor)
  } toMap

  private lazy val packetTypes: Map[Int, Type] = classTypes collect {
    case tpe if hasAnnotation[packet](tpe.typeSymbol) =>
      val ann = annotation[packet](tpe.typeSymbol)
      annotationParam[Int](ann, 0) -> tpe
  } toMap

  private lazy val packetIds: Map[Type, Int] = packetTypes map { _.swap }

  private lazy val packetMarshallers: Map[Int, Marshaller[Structure]] = packetTypes collect {
    case (packetId, tpe) => packetId -> createMarshaller(tpe)
  } toMap

  def marshal(packet: Structure)(implicit outStream: BufferedOutputStream): Unit =
    packetMarshallers(packetIds(runtimeType(packet))).marshal(packet)

  def unmarshal(packetId: Int)(implicit inStream: BufferedInputStream): Structure =
    packetMarshallers(packetId).unmarshal()

  private def runtimeType[P: TypeTag](obj: P): Type = typeTag[T].tpe

  private def createMarshaller(tpe: Type): Marshaller[Structure] = {
    val paramSymbols = tpe.decl(termNames.CONSTRUCTOR).asMethod.paramLists(0)
    val paramMarshallers = paramSymbols map {
      case sym if isSymType[Int](sym) =>
        if (hasAnnotation[byte](sym)) {
          ByteMarshaller
        } else if (hasAnnotation[short](sym)) {
          ShortMarshaller
        } else if (hasAnnotation[packed](sym)) {
          VarIntMarshaller
        } else {
          IntMarshaller
        }
      case sym if isSymType[Long](sym) => LongMarshaller
      case sym if isSymType[VarInt](sym) => VarIntMarshaller
      case sym if isSymType[String](sym) =>
        if (hasAnnotation[maxLength](sym)) {
          new StringMarshaller(annotationParam[Int](annotation(sym), 0))
        } else {
          new StringMarshaller(MaxStringLength)
        }
      case sym if isSymType[Boolean](sym) => BooleanMarshaller
      case sym if isSymType[UUID](sym) => UUIDMarshaller
      case sym if isSymType[Option[_]](sym) => new OptionalMarshaller(createMarshaller(sym.info.typeArgs(0)))
      case sym if isSymType[Array[_]](sym) && hasAnnotation[precededBy[_]](sym) =>
        new ArrayMarshaller(createMarshaller(sym.info.typeArgs(0)), VarIntMarshaller)
    } map { _.asInstanceOf[Marshaller[Any]]}

    new StructureMarshaller(paramMarshallers, classConstructors(tpe))
  }

  private def isSymType[U: TypeTag](symbol: Symbol): Boolean = symbol.info <:< typeOf[U]
  private def hasAnnotation[U: TypeTag](symbol: Symbol): Boolean = symbol.annotations.exists {_.tree.tpe =:= typeOf[U]}
  private def annotation[U: TypeTag](symbol: Symbol): Annotation =
    symbol.annotations.find {_.tree.tpe =:= typeOf[packet]}.get
  private def annotationParams(annotation: Annotation, index: Int): List[Any] = annotation.tree.children.tail map {
    case Literal(Constant(value)) => value
  }
  private def annotationParam[U](annotation: Annotation, index: Int): U =
    annotationParams(annotation, index).asInstanceOf[U]



}