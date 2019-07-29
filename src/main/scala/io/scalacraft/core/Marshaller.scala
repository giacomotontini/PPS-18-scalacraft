package io.scalacraft.core

import java.io.{BufferedInputStream, BufferedOutputStream}

trait Marshaller[T] {
  def marshal(obj: T)(implicit outStream: BufferedOutputStream): Unit
  def unmarshal()(implicit inStream: BufferedInputStream): T
}