package io.scalacraft.core

import java.io.{BufferedInputStream, BufferedOutputStream}
import java.util.UUID

import io.scalacraft.core.DataTypes.VarInt
import io.scalacraft.core.Marshallers._
import io.scalacraft.core.PacketAnnotations._
import io.scalacraft.core.serverbound.PlayPackets

import scala.language.postfixOps
import scala.reflect.runtime.universe._

class PacketManager {

  private val mirror = runtimeMirror(getClass.getClassLoader)

  private val classTypes: List[ClassSymbol] = typeOf[PlayPackets.type].decls.collect {
    case sym if sym.isClass && !sym.isAbstract => sym.asClass
  } toList

  private val classConstructors: Map[Type, MethodMirror] = classTypes map { sym =>
    val cm = mirror.reflectClass(sym)
    val ctor = sym.toType.decl(termNames.CONSTRUCTOR).asMethod
    sym.toType -> cm.reflectConstructor(ctor)
  } toMap

  private val packetTypes: Map[Int, Type] = classTypes collect {
    case sym if hasAnnotation[packet](sym) =>
      val ann = annotation[packet](sym)
      annotationParam[Int](ann, 0) -> sym.toType
  } toMap

  private val packetMarshallers: Map[Int, Marshaller[Structure]] = packetTypes map {
    case (packetId, tpe) => packetId -> createMarshaller(tpe)
  }

  def marshal[T <: Structure: TypeTag](packet: T)(implicit outStream: BufferedOutputStream): Unit = {
    val packetId = packetTypes.collectFirst {
      case (packetId, tpe) if tpe =:= runtimeType[T](packet) => packetId
    } get

    packetMarshallers(packetId).marshal(packet)
  }

  def unmarshal(packetId: Int)(implicit inStream: BufferedInputStream): Structure =
    packetMarshallers(packetId).unmarshal()

  private def createMarshaller(tpe: Type): Marshaller[Structure] = {
    // 0 is to take the first curring arguments list
    val paramSymbols = tpe.decl(termNames.CONSTRUCTOR).asMethod.paramLists(0)
    val paramMarshallers = paramSymbols map subTypesMarshaller map { _.asInstanceOf[Marshaller[Any]]}
    new StructureMarshaller(paramMarshallers, classConstructors(tpe))
  }

  private def subTypesMarshaller: PartialFunction[Symbol, Marshaller[_]] = {
    case sym => subTypesMarshaller(checkAnnotations = true, Some(sym))(sym)
  }

  private def subTypesMarshaller(checkAnnotations: Boolean, symAnnotation: Option[Symbol] = None)
                                (symbol: Symbol): Marshaller[_] = {
    symbol match {
      case sym if isSymType[Int](sym) && checkAnnotations =>
        if (hasAnnotation[byte](symAnnotation.get)) {
          ByteMarshaller
        } else if (hasAnnotation[short](symAnnotation.get)) {
          ShortMarshaller
        } else if (hasAnnotation[boxed](symAnnotation.get)) {
          VarIntMarshaller
        } else {
          IntMarshaller
        }
      case sym if isSymType[Int](sym) => IntMarshaller
      case sym if isSymType[Boolean](sym) => BooleanMarshaller
      case sym if isSymType[Long](sym) => LongMarshaller
      case sym if isSymType[UUID](sym) => UUIDMarshaller
      case sym if isSymType[VarInt](sym) => VarIntMarshaller
      case sym if isSymType[String](sym) && checkAnnotations && hasAnnotation[maxLength](symAnnotation.get) =>
        new StringMarshaller(annotationParam[Int](annotation[maxLength](symAnnotation.get), 0))
      case sym if isSymType[String](sym) => new StringMarshaller(MaxStringLength)
      case sym if isSymType[Option[_]](sym) =>
        val paramMarshaller = subTypesMarshaller(checkAnnotations = true, Some(sym))(sym.info.typeArgs(0).typeSymbol)
        new OptionalMarshaller(paramMarshaller)
      case sym if isSymType[Array[_]](sym) && hasAnnotation[precededBy[_]](sym) =>
        val precededByAnnotation = annotation[precededBy[_]](sym)
        val precededByType = precededByAnnotation.tree.children(0).tpe.typeArgs(0).typeSymbol
        val precededByMarshaller = subTypesMarshaller(checkAnnotations = false)(precededByType)
        val paramMarshaller = subTypesMarshaller(checkAnnotations = true, Some(sym))(sym.info.typeArgs(0).typeSymbol)
        new ArrayMarshaller(paramMarshaller, precededByMarshaller.asInstanceOf[Marshaller[Int]])
      case sym => createMarshaller(sym.asType.toType)
    }
  }

  private def runtimeType[P: TypeTag](obj: P): Type = typeTag[P].tpe
  private def isSymType[U: TypeTag](symbol: Symbol): Boolean =
    (if (symbol.isType) symbol.asType.toType else symbol.info) <:< typeOf[U]
  private def hasAnnotation[U: TypeTag](symbol: Symbol): Boolean = symbol.annotations.exists {_.tree.tpe =:= typeOf[U]}
  private def annotation[U: TypeTag](symbol: Symbol): Annotation =
    symbol.annotations.find {_.tree.tpe =:= typeOf[U]}.get
  private def annotationParams(annotation: Annotation): List[Any] = annotation.tree.children.tail map {
    case Literal(Constant(value)) => value
  }
  private def annotationParam[U](annotation: Annotation, index: Int): U =
    annotationParams(annotation)(index).asInstanceOf[U]

}