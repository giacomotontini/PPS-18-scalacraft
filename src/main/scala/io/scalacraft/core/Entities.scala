package io.scalacraft.core

import io.scalacraft.core.DataTypes.{Chat, Position, Slot}
import io.scalacraft.core.PacketAnnotations._
import io.scalacraft.core.serverbound.PlayPackets.{ParticleData, minecraftEffect}


object Entities {

  trait MobEntity extends Entity
  trait ObjectEntity extends Entity

  class Entity extends EntityMetadata {
    @indexType(index = 0) @byte var status: Int = 0
    @indexType(index = 1) @boxed var air: Int = 300
    @indexType(index = 5) var customName: Option[Chat] = None
    @indexType(index = 7) var isCustomNameVisible: Boolean = false
    @indexType(index = 7) var isSilent: Boolean = false
    @indexType(index = 7) var noGravity: Boolean = false
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

  class AreaEffectCloud extends Entity with MobEntity{
    @indexType(index = 2) val radius: Float = 0.5f
    @indexType(index = 1) @boxed val color: Int = 0
    @indexType(index = 7) val ignoreRadiusAndShowEffectAtSinglePoint = false
    @indexType(index = 15) val particle: ParticleData = minecraftEffect()
  }
}
