package io.scalacraft.core.marshalling

import java.io.{DataInputStream, DataOutputStream}

trait Marshaller {

  protected[this] def contextFieldIndex: Option[Int]

  def marshal(obj: Any)(implicit outStream: DataOutputStream): Unit

  def unmarshal()(implicit context: Context, inStream: DataInputStream): Any = contextFieldIndex match {
    case Some(index) => context.field(index)
    case _ => internalUnmarshal()
  }

  protected[this] def internalUnmarshal()(implicit context: Context, inStream: DataInputStream): Any

}
