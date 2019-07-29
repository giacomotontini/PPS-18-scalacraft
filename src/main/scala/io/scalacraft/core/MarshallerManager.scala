package io.scalacraft.core

import java.util.UUID
import scala.reflect.runtime.universe.{Type, typeOf}
import Marshallers._

object MarshallerManager {

  private var marshallers: Map[(Type, List[Any]), Marshaller[Any]] = Map(
    (typeOf[Boolean], List.empty) -> BooleanMarshaller,
    (typeOf[Byte], List.empty) -> ByteMarshaller,
    (typeOf[Short], List.empty) -> ShortMarshaller,
    (typeOf[Int], List.empty) -> IntMarshaller,
    (typeOf[Long], List.empty) -> LongMarshaller,
    (typeOf[UUID], List.empty) -> UUIDMarshaller
  ) map { t => (t._1, t._2.asInstanceOf[Marshaller[Any]]) }

  def marshallerFor(objType: Type, params: List[Any] = List.empty): Marshaller[Any] = {
    if (marshallers.contains((objType, params)) {
      marshallers(objType)
    } else {


    }
  }



}
