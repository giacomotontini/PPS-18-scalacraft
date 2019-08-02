package io.scalacraft.core

import io.scalacraft.core.DataTypes.{Chat, Slot}
import io.scalacraft.core.PacketAnnotations._


object Entities {

  //class typeIndex(index: Int) extends StaticAnnotation

  class Entity extends EntityMetadata {
    @indexType(index = 0) @byte var status: Int = 0
    @indexType(index = 1) @boxed var air: Int = 300
    //var customName: Option[Chat] = None
    //var isCustomNameVisible: Boolean = false
    //var isSilent: Boolean = false
    //var noGravity: Boolean = false
  }

  class Throwable extends Entity

  class Egg extends  Throwable

  class EnderPearl extends Throwable

  class ExperienceBottle extends Throwable

  class Snowball extends Throwable

  class Position extends Throwable {
    var positionWhichIsThrown: Slot = None
  }
}
