package io.scalacraft.core

import scala.annotation.StaticAnnotation
import scala.reflect.runtime.universe.Type

object PacketAnnotations {
  val MaxStringLength: Int = 32767

  class packet(id: Int) extends StaticAnnotation
  class packed extends StaticAnnotation
  class byte extends StaticAnnotation
  class short extends StaticAnnotation
  class enum(values: Any*) extends StaticAnnotation
  class maxLength(value: Int) extends StaticAnnotation
  class switch[K](options: (K, Type)*) extends StaticAnnotation
  class precededBy[T]() extends StaticAnnotation
}
