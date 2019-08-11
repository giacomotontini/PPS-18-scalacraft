package io.scalacraft.core.marshalling.annotations

import scala.annotation.StaticAnnotation

private[scalacraft] object PacketAnnotations {
  val MaxStringLength: Int = 32767

  class packet(id: Int) extends StaticAnnotation
  class boxed extends StaticAnnotation
  class byte extends StaticAnnotation
  class short extends StaticAnnotation
  class maxLength(value: Int) extends StaticAnnotation
  class switchType[K] extends StaticAnnotation
  class switchKey(value: Any) extends StaticAnnotation
  class precededBy[T] extends StaticAnnotation
  class enumType[K] extends StaticAnnotation
  class enumValue(value: Any) extends StaticAnnotation
  class particle(id: Int) extends StaticAnnotation
  class fromContext(index: Int) extends StaticAnnotation

}