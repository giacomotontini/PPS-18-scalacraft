package io.scalacraft.core

import java.io.{BufferedInputStream, BufferedOutputStream}

trait Marshaller {
  def marshal(obj: Any)(implicit outStream: BufferedOutputStream): Unit
  def unmarshal()(implicit inStream: BufferedInputStream): Any
}