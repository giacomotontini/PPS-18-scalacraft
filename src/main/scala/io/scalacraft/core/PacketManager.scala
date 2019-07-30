package io.scalacraft.core

import java.io.{BufferedInputStream, BufferedOutputStream}
import java.util.UUID

import io.scalacraft.core.DataTypes.VarInt
import io.scalacraft.core.Marshallers._
import io.scalacraft.core.PacketAnnotations._
import io.scalacraft.core.serverbound.PlayPackets
import io.scalacraft.core.serverbound.PlayPackets.AddPlayerProperty

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
      case (packetId, tpe) if tpe =:= Helpers.runtimeType[T](packet) => packetId
    } get

    packetMarshallers(packetId).marshal(packet)
  }

  def unmarshal(packetId: Int)(implicit inStream: BufferedInputStream): Structure =
    packetMarshallers(packetId).unmarshal()

  private def createMarshaller(tpe: Type): Marshaller[Structure] = {
    // 0 is to take the first curring arguments list
    val paramSymbols = tpe.decl(termNames.CONSTRUCTOR).asMethod.paramLists.head
    val paramMarshallers = paramSymbols map subTypesMarshaller map { _.asInstanceOf[Marshaller[Any]]}
    new StructureMarshaller(paramMarshallers, classConstructors(tpe))
  }

  private def subTypesMarshaller: PartialFunction[Symbol, Marshaller[_]] = {
    case sym => subTypesMarshaller(checkAnnotations = true, Some(sym))(sym)
  }

  private def subTypesMarshaller[Any](checkAnnotations: Boolean, symAnnotations: Option[Symbol] = None)
                                (symbol: Symbol): Marshaller[_] = {
    symbol match {
      case sym if checkAnnotations && hasAnnotation[switchType[_]](symAnnotations.get) =>
        val keyType = annotationTypeArg(annotation[switchType[_]](symAnnotations.get), 0)
        val keyMarshaller = subTypesMarshaller(checkAnnotations = false)(keyType).asInstanceOf[Marshaller[Any]]

        val switchTrait = if (isSymType[Array[_]](sym)) {
          sym.info.typeArgs.head.typeSymbol
        } else sym

        val valuesType = classTypes collect {
          case sym if hasAnnotation[switchKey](sym) && sym.baseClasses.contains(switchTrait) =>
            val ann = annotation[switchKey](sym)
            annotationParam[Any](ann, 0) -> sym.toType
        } toMap

        var valuesMarshaller = valuesType map {
          case (keyId, tpe) =>
            keyId -> createMarshaller(tpe).asInstanceOf[Marshaller[Any]]
        }

        if (isSymType[Array[_]](sym)) {
          val precededByType = annotationTypeArg(annotation[precededBy[_]](symAnnotations.get), 0)
          val precededByMarshaller = subTypesMarshaller(checkAnnotations = false)(precededByType)

          valuesMarshaller = valuesMarshaller map {
            case (keyId, marshaller) =>
              keyId -> new ArrayMarshaller(marshaller, precededByMarshaller.asInstanceOf[Marshaller[Int]])
                .asInstanceOf[Marshaller[Any]]
          }
        }

        new SwitchMarshaller(keyMarshaller, valuesMarshaller, valuesType.map { _.swap })
      case sym if isSymType[Int](sym) && checkAnnotations =>
        if (hasAnnotation[byte](symAnnotations.get)) {
          ByteMarshaller
        } else if (hasAnnotation[short](symAnnotations.get)) {
          ShortMarshaller
        } else if (hasAnnotation[boxed](symAnnotations.get)) {
          VarIntMarshaller
        } else {
          IntMarshaller
        }
      case sym if isSymType[Int](sym) => IntMarshaller
      case sym if isSymType[Boolean](sym) => BooleanMarshaller
      case sym if isSymType[Long](sym) => LongMarshaller
      case sym if isSymType[UUID](sym) => UUIDMarshaller
      case sym if isSymType[VarInt](sym) => VarIntMarshaller
      case sym if isSymType[String](sym) && checkAnnotations && hasAnnotation[maxLength](symAnnotations.get) =>
        new StringMarshaller(annotationParam[Int](annotation[maxLength](symAnnotations.get), 0))
      case sym if isSymType[String](sym) => new StringMarshaller(MaxStringLength)
      case sym if isSymType[Option[_]](sym) =>
        val paramMarshaller = subTypesMarshaller(checkAnnotations = true, Some(sym))(sym.info.typeArgs.head.typeSymbol)
        new OptionalMarshaller(paramMarshaller)
      case sym if isSymType[Array[_]](sym) && checkAnnotations && hasAnnotation[precededBy[_]](symAnnotations.get) =>
        val precededByType = annotationTypeArg(annotation[precededBy[_]](symAnnotations.get), 0)
        val precededByMarshaller = subTypesMarshaller(checkAnnotations = false)(precededByType)
        val paramMarshaller = subTypesMarshaller(checkAnnotations = true, Some(sym))(sym.info.typeArgs.head.typeSymbol)
        new ArrayMarshaller(paramMarshaller, precededByMarshaller.asInstanceOf[Marshaller[Int]])
      case sym => createMarshaller(sym.asType.toType)
    }
  }

  private def isSymType[U: TypeTag](symbol: Symbol): Boolean =
    (if (symbol.isType) symbol.asType.toType else symbol.info) <:< typeOf[U]
  private def hasAnnotation[U: TypeTag](symbol: Symbol): Boolean = symbol.annotations.exists {_.tree.tpe <:< typeOf[U]}
  private def annotation[U: TypeTag](symbol: Symbol): Annotation =
    symbol.annotations.find {_.tree.tpe <:< typeOf[U]}.get
  private def annotationParams(annotation: Annotation): List[Any] = annotation.tree.children.tail map {
    case Literal(Constant(value)) => value
  }
  private def annotationParam[U](annotation: Annotation, index: Int): U =
    annotationParams(annotation)(index).asInstanceOf[U]

  private def annotationTypeArg(annotation: Annotation, index: Int): Symbol = {
    val tree = annotation.tree.withFilter(t => t.isType).head
    tree.tpe.typeArgs(index).typeSymbol
  }

}