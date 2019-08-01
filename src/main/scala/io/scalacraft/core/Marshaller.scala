package io.scalacraft.core

import java.io.{BufferedInputStream, BufferedOutputStream}

trait Marshaller {

  def contextFieldIndex: Option[Int]

  def marshal(obj: Any)(implicit outStream: BufferedOutputStream): Unit

  def unmarshal()(implicit context: Context, inStream: BufferedInputStream): Any = contextFieldIndex match {
    case Some(index) => context.field(index)
    case _ => internalUnmarshal()
  }

  protected[this] def internalUnmarshal()(implicit context: Context, inStream: BufferedInputStream): Any

}
