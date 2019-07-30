package io.scalacraft.core

import scala.annotation.StaticAnnotation

object PacketAnnotations {
  val MaxStringLength: Int = 32767

  class packet(id: Int) extends StaticAnnotation
  class boxed extends StaticAnnotation
  class byte extends StaticAnnotation
  class short extends StaticAnnotation
  class enum(values: Any*) extends StaticAnnotation
  class maxLength(value: Int) extends StaticAnnotation
  class switch[K](options: (K, Class[_])*) extends StaticAnnotation
  class precededBy[T]() extends StaticAnnotation
}
