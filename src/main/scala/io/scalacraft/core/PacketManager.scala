package io.scalacraft.core

import java.io.{BufferedInputStream, BufferedOutputStream}
import java.util.UUID

import io.scalacraft.core.DataTypes.{Position => _, _}
import io.scalacraft.core.Entities.{Entity, MobEntity, ObjectEntity, Player}
import io.scalacraft.core.Marshallers._
import io.scalacraft.core.MobsAndObjectsTypeMapping._
import io.scalacraft.core.PacketAnnotations._

import scala.language.postfixOps
import scala.reflect.runtime.universe._

class PacketManager[T: TypeTag] {

  private val mirror = runtimeMirror(getClass.getClassLoader)

  private val classTypes: List[ClassSymbol] = loadClassTypes[T]() ++ loadClassTypes[DataTypes.type]() ++
    loadClassTypes[Entities.type]()

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

  def marshal[U <: Structure](packet: U)(implicit outStream: BufferedOutputStream): Unit = {
    val packetId = packetTypes.collectFirst {
      case (packetId, tpe) if tpe =:= mirror.classSymbol(packet.getClass).toType => packetId
    } get

    packetMarshallers(packetId).marshal(packet)
  }

  def unmarshal(packetId: Int)(implicit inStream: BufferedInputStream): Structure =
    packetMarshallers(packetId).unmarshal()(Context.create, inStream).asInstanceOf[Structure]

  private def getParamMarshallers(tpe: Type): List[Marshaller] = {
    val paramSymbols = tpe.decl(termNames.CONSTRUCTOR).asMethod.paramLists.head
    paramSymbols map subTypesMarshaller
  }
  private def createMarshaller(tpe: Type): Marshaller = {
    // 0 is to take the first curring arguments list
    new StructureMarshaller(getParamMarshallers(tpe), classConstructors(tpe))
  }

  private def subTypesMarshaller: PartialFunction[Symbol, Marshaller] = {
    case sym => subTypesMarshaller(checkAnnotations = true, Some(sym), Some(sym))(sym)
  }

  private def subTypesMarshaller(checkAnnotations: Boolean,
                                 symAnnotations: Option[Symbol] = None,
                                 contextAnnotation: Option[Symbol] = None)
                                (symbol: Symbol): Marshaller = {

    val contextFieldIndex = if (contextAnnotation.isDefined && hasAnnotation[fromContext](contextAnnotation.get)) {
      Some(annotationParam[Int](annotation[fromContext](contextAnnotation.get), 0))
    } else None

    symbol match {
      case sym if checkAnnotations && hasAnnotation[switchType[_]](symAnnotations.get) =>
        val keyType = annotationTypeArg(annotation[switchType[_]](symAnnotations.get), 0)
        val keyMarshaller = if (contextFieldIndex.isDefined) {
          subTypesMarshaller(checkAnnotations = false, contextAnnotation = Some(sym))(keyType)
        } else {
          subTypesMarshaller(checkAnnotations = false, contextAnnotation = Some(sym))(keyType)
        }

        val switchTrait = if (isSymType[List[_]](sym) || isSymType[Option[_]](sym)) {
          sym.info.typeArgs.head.typeSymbol
        } else sym.info.typeSymbol

        val valuesType = classTypes collect {
          case sym if hasAnnotation[switchKey](sym) && sym.baseClasses.contains(switchTrait) =>
            val ann = annotation[switchKey](sym)
            annotationParam[Any](ann, 0) -> sym.toType
        } toMap

        var valuesMarshaller = valuesType map {
          case (keyId, tpe) =>
            keyId -> createMarshaller(tpe)
        }

        if (isSymType[List[_]](sym)) {
          val precededByType = annotationTypeArg(annotation[precededBy[_]](symAnnotations.get), 0)
          val precededByMarshaller = subTypesMarshaller(checkAnnotations = false)(precededByType)

          valuesMarshaller = valuesMarshaller map {
            case (keyId, marshaller) =>
              keyId -> new ListMarshaller(marshaller, Some(precededByMarshaller))
          }
        } else if (isSymType[Option[_]](sym)) {
          valuesMarshaller = valuesMarshaller map {
            case (keyId, marshaller) =>
              keyId -> new OptionalMarshaller(marshaller)
          }
        }

        val valuesClazzes: Map[RuntimeClass, Any] = valuesType.map {
          case (key, tpe) => mirror.runtimeClass(tpe) -> key
        }.toMap // don't remove .toMap to avoid type mismatch error

        new SwitchMarshaller(keyMarshaller, valuesMarshaller, valuesClazzes, contextFieldIndex.isDefined)
      case sym if isSymType[Option[_]](sym) =>
        val argType = if (isSymType[Slot](sym)) typeOf[SlotData].typeSymbol else sym.info.typeArgs.head.typeSymbol
        val paramMarshaller = subTypesMarshaller(checkAnnotations = true, Some(sym))(argType)
        val conditionMarshaller = contextFieldIndex map { i => new BooleanMarshaller(Some(i)) }
        new OptionalMarshaller(paramMarshaller, conditionMarshaller)
      case sym if isSymType[Int](sym) && checkAnnotations =>
        if (hasAnnotation[byte](symAnnotations.get)) {
          new ByteMarshaller(false, contextFieldIndex)
        } else if (hasAnnotation[short](symAnnotations.get)) {
          new ShortMarshaller(false, contextFieldIndex)
        } else if (hasAnnotation[boxed](symAnnotations.get)) {
          new VarIntMarshaller(contextFieldIndex)
        } else {
          new IntMarshaller(contextFieldIndex)
        }
      case sym if isSymType[Long](sym) && checkAnnotations =>
        if (hasAnnotation[boxed](symAnnotations.get)) {
          new VarLongMarshaller(contextFieldIndex)
        } else {
          new LongMarshaller(contextFieldIndex)
        }
      case sym if isSymType[Int](sym) => new IntMarshaller(contextFieldIndex)
      case sym if isSymType[Boolean](sym) => new BooleanMarshaller(contextFieldIndex)
      case sym if isSymType[Long](sym) => new LongMarshaller(contextFieldIndex)
      case sym if isSymType[Float](sym) => new FloatMarshaller(contextFieldIndex)
      case sym if isSymType[Double](sym) => new DoubleMarshaller(contextFieldIndex)
      case sym if isSymType[DataTypes.Position](sym) => new PositionMarshaller(contextFieldIndex)
      case sym if isSymType[UUID](sym) => new UUIDMarshaller(contextFieldIndex)
      case sym if isSymType[VarInt](sym) => new VarIntMarshaller(contextFieldIndex)
      case sym if isSymType[Nbt](sym) => new NbtMarshaller(contextFieldIndex)
      case sym if isSymType[String](sym) && checkAnnotations && hasAnnotation[maxLength](symAnnotations.get) =>
        new StringMarshaller(annotationParam[Int](annotation[maxLength](symAnnotations.get), 0), contextFieldIndex)
      case sym if isSymType[String](sym) => new StringMarshaller(MaxStringLength, contextFieldIndex)
      case sym if isSymType[List[_]](sym) /*&& checkAnnotations && hasAnnotation[precededBy[_]](symAnnotations.get)*/ =>
        val precededByMarshaller = if( checkAnnotations && hasAnnotation[precededBy[_]](symAnnotations.get)) {
          val precededByType = annotationTypeArg(annotation[precededBy[_]](symAnnotations.get), 0)
          Some(subTypesMarshaller(checkAnnotations = false)(precededByType))
        } else None
        val paramMarshaller = subTypesMarshaller(checkAnnotations = true, Some(sym))(sym.info.typeArgs.head.typeSymbol)
        new ListMarshaller(paramMarshaller, precededByMarshaller, contextFieldIndex)
      case sym if isSymType[Entity](sym) =>
        def getTypeToEntityConstructorMap(typeToEntityClass: Map[Int, Class[_]]): Map[Int, MethodMirror] = {
          typeToEntityClass map{
            case (index, clazz) => index -> classConstructors(mirror.classSymbol(clazz).toType)
          }
        }
        var typeToEntityClassConstructor: Map[Int, MethodMirror] = Map()
        val typesMarshaller = getParamMarshallers(typeOf[entityMetadataTypes])
        val typeMarshaller = new VarIntMarshaller(contextFieldIndex)
        if (isSymType[MobEntity](sym)){
          typeToEntityClassConstructor = getTypeToEntityConstructorMap(typeToMobEntityClass)
        } else if(isSymType[ObjectEntity](sym)){
          typeToEntityClassConstructor = getTypeToEntityConstructorMap(typeToObjectEntityClass)
        }
        new EntityMarshaller(typeToEntityClassConstructor, typeMarshaller, typesMarshaller)
      case sym if checkAnnotations && hasAnnotation[enumType[_]](symAnnotations.get) =>
        val valueType = annotationTypeArg(annotation[enumType[_]](symAnnotations.get), 0)
        val valueMarshaller = subTypesMarshaller(checkAnnotations = false, contextAnnotation=Some(sym))(valueType)
        val companionSymbol = sym.info.typeSymbol.companion

        val valuesInstances = companionSymbol.info.decls collect {
          case memberSymbol if memberSymbol.isModule => memberSymbol.asModule
        } collect {
          case sym if hasAnnotation[enumValue](sym) && sym.alternatives.contains(sym) =>
            val ann = annotation[enumValue](sym)
            annotationParam[Any](ann, 0) -> moduleInstance(sym.info)
        } toMap

        new EnumMarshaller(valueMarshaller, valuesInstances)
      case sym => createMarshaller(if (sym.isType) sym.asType.toType else sym.info)
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

  private def moduleInstance(tpe: Type): Any = {
      val module = tpe.typeSymbol.companionSymbol.asModule
      mirror.reflectModule(module).instance
  }

}