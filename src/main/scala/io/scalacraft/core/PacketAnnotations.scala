package io.scalacraft.core

import scala.annotation.StaticAnnotation

object PacketAnnotations {
  class packet(id: Int) extends StaticAnnotation
  class packed extends StaticAnnotation
  class byte extends StaticAnnotation
  class short extends StaticAnnotation
  class enum(values: Any*) extends StaticAnnotation
  class maxLength(value: Int) extends StaticAnnotation
  class switch[K,V](values: Map[K, V]) extends StaticAnnotation
  class precededBy[T]() extends StaticAnnotation
}
