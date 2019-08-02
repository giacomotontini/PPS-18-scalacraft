package io.scalacraft.core

import io.scalacraft.core.DataTypes.{Chat, Position, Slot}
import io.scalacraft.core.PacketAnnotations._
import io.scalacraft.core.serverbound.PlayPackets.{Particle, ParticleData, minecraftEffect}


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

  class Potion extends Throwable {
    var potionWhichIsThrown: Slot = None
  }

  class EyeOfEnder extends Entity

  class FallingBlock extends Entity {
    @indexType(index = 9) val spawnPosition: Position = Position(0,0,0)
  }

  class AreaEffectCloud extends Entity {
    @indexType() val radius: Float = 0.5f
    @indexType() @boxed val color: Int = 0
    @indexType() val ignoreRadiusAndShowEffectAtSinglePoint = false
    @indexType() val particle: ParticleData = minecraftEffect()
  }
}
