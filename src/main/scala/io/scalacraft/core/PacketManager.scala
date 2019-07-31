package io.scalacraft.core

import java.io.{BufferedInputStream, BufferedOutputStream}
import java.util.UUID


import io.scalacraft.core.DataTypes.{Slot, SlotData, VarInt, Nbt}
import io.scalacraft.core.Marshallers._
import io.scalacraft.core.PacketAnnotations._

import scala.language.postfixOps
import scala.reflect.runtime.universe._
import scala.reflect.runtime.universe.compat.token

class PacketManager[T: TypeTag] {

  private val mirror = runtimeMirror(getClass.getClassLoader)

  private val classTypes: List[ClassSymbol] = loadClassTypes[T]() ++ loadClassTypes[DataTypes.type ]()

  private def loadClassTypes[U: TypeTag](): List[ClassSymbol] = {
    typeOf[U].decls.collect {
      case sym if sym.isClass && !sym.isAbstract => sym.asClass
    } toList
  }

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

  private val packetMarshallers: Map[Int, Marshaller] = packetTypes map {
    case (packetId, tpe) => packetId -> createMarshaller(tpe)
  }

  def marshal[U <: Structure: TypeTag](packet: U)(implicit outStream: BufferedOutputStream): Unit = {
    val packetId = packetTypes.collectFirst {
      case (packetId, tpe) if tpe =:= Helpers.runtimeType[U](packet) => packetId
    } get

    packetMarshallers(packetId).marshal(packet)
  }

  def unmarshal(packetId: Int)(implicit inStream: BufferedInputStream): Structure =
    packetMarshallers(packetId).unmarshal().asInstanceOf[Structure]

  private def createMarshaller(tpe: Type): Marshaller = {
    // 0 is to take the first curring arguments list
    val paramSymbols = tpe.decl(termNames.CONSTRUCTOR).asMethod.paramLists.head
    val paramMarshallers = paramSymbols map subTypesMarshaller
    new StructureMarshaller(paramMarshallers, classConstructors(tpe))
  }

  private def subTypesMarshaller: PartialFunction[Symbol, Marshaller] = {
    case sym => subTypesMarshaller(checkAnnotations = true, Some(sym))(sym)
  }

  private def subTypesMarshaller(checkAnnotations: Boolean, symAnnotations: Option[Symbol] = None)
                                (symbol: Symbol): Marshaller = {
    symbol match {
      case sym if checkAnnotations && hasAnnotation[switchType[_]](symAnnotations.get) =>
        val keyType = annotationTypeArg(annotation[switchType[_]](symAnnotations.get), 0)
        val keyMarshaller = subTypesMarshaller(checkAnnotations = false)(keyType)

        val switchTrait = if (isSymType[Array[_]](sym)) {
          sym.info.typeArgs.head.typeSymbol
        } else sym

        var valuesType = classTypes collect {
          case sym if hasAnnotation[switchKey](sym) && sym.baseClasses.contains(switchTrait) =>
            val ann = annotation[switchKey](sym)
            annotationParam[Any](ann, 0) -> sym.toType
        } toMap

        var valuesMarshaller = valuesType map {
          case (keyId, tpe) =>
            keyId -> createMarshaller(tpe)
        }

        if (isSymType[Array[_]](sym)) {
          val precededByType = annotationTypeArg(annotation[precededBy[_]](symAnnotations.get), 0)
          val precededByMarshaller = subTypesMarshaller(checkAnnotations = false)(precededByType)

          val valuesWithArrayType = valuesType.map {
            case (key, tpe) => key -> (sym.info match {
              case TypeRef(p, sym, _) => TypeRef(p, sym, List(tpe))
            })
          }

          valuesMarshaller = valuesMarshaller map {
            case (keyId, marshaller) =>
              val runtimeClass = mirror.runtimeClass(valuesType(keyId).typeSymbol.asClass)
              keyId -> new ArrayMarshaller(marshaller, Some(precededByMarshaller), runtimeClass)
          }

          valuesType = valuesWithArrayType
        }

        val valuesClazzes: Map[RuntimeClass, Any] = valuesType.map {
          case (key, tpe) => mirror.runtimeClass(tpe) -> key
        }.toMap // don't remove .toMap to avoid type mismatch error

        new SwitchMarshaller(keyMarshaller, valuesMarshaller, valuesClazzes)
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
      case sym if isSymType[Float](sym) => FloatMarshaller
      case sym if isSymType[Double](sym) => DoubleMarshaller
      case sym if isSymType[Position](sym) => PositionMarshaller
      case sym if isSymType[UUID](sym) => UUIDMarshaller
      case sym if isSymType[VarInt](sym) => VarIntMarshaller
      case sym if isSymType[Nbt](sym) => NbtMarshaller
      //case sym if isSymType[Slot](sym)  => SlotMarshaller
      case sym if isSymType[String](sym) && checkAnnotations && hasAnnotation[maxLength](symAnnotations.get) =>
        new StringMarshaller(annotationParam[Int](annotation[maxLength](symAnnotations.get), 0))
      case sym if isSymType[String](sym) => new StringMarshaller(MaxStringLength)
      case sym if isSymType[Option[_]](sym) =>
        val argType = if(isSymType[Slot](sym)) typeOf[SlotData].typeSymbol else sym.info.typeArgs.head.typeSymbol
        val paramMarshaller = subTypesMarshaller(checkAnnotations = true, Some(sym))(argType)
        new OptionalMarshaller(paramMarshaller)
      case sym if isSymType[Array[_]](sym) && checkAnnotations && hasAnnotation[precededBy[_]](symAnnotations.get) =>
        val precededByType = annotationTypeArg(annotation[precededBy[_]](symAnnotations.get), 0)
        val precededByMarshaller = subTypesMarshaller(checkAnnotations = false)(precededByType)
        val paramMarshaller = subTypesMarshaller(checkAnnotations = true, Some(sym))(sym.info.typeArgs.head.typeSymbol)
        val runtimeClass = mirror.runtimeClass(sym.info.typeArgs.head.typeSymbol.asClass)
        new ArrayMarshaller(paramMarshaller, Some(precededByMarshaller), runtimeClass)
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