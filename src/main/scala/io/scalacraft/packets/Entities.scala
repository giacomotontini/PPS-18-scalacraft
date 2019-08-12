package io.scalacraft.packets

import java.util.UUID

import io.scalacraft.core.marshalling.EntityMetadata
import io.scalacraft.core.marshalling.annotations.PacketAnnotations.{boxed, byte}
import io.scalacraft.core.marshalling.annotations.indexType
import io.scalacraft.packets.DataTypes._
import net.querz.nbt.CompoundTag

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

  class Egg extends  Throwable with MobEntity with ObjectEntity

  class EnderPearl extends Throwable with MobEntity with ObjectEntity

  class ExperienceBottle extends Throwable with MobEntity with ObjectEntity

  class Snowball extends Throwable with MobEntity with ObjectEntity

  class Potion extends Throwable with MobEntity with ObjectEntity {
    var potionWhichIsThrown: Slot = None
  }

  class EyeOfEnder extends Entity with ObjectEntity

  class FallingBlock extends Entity with ObjectEntity {
    @indexType(index = 9) var spawnPosition: Position = Position(0,0,0)
  }

  class AreaEffectCloud extends Entity with MobEntity with ObjectEntity {
    @indexType(index = 2) var radius: Float = 0.5f
    @indexType(index = 1) @boxed var color: Int = 0
    @indexType(index = 7) var ignoreRadiusAndShowEffectAtSinglePoint: Boolean = false
    @indexType(index = 15) var particle: ParticleData = Effect()
  }

  class FishingHook extends Entity with ObjectEntity {
    @indexType(index = 1) @boxed var hookedEntity: Int = 0
  }

  class Arrow extends Entity with MobEntity {
    @indexType(index = 0) @byte var arrowStatus: Int = 0
    @indexType(index = 12) var shooterUUID: Option[UUID]= None
  }

  class TippedArrow extends Arrow with ObjectEntity {
    @indexType(index = 1) @boxed var color: Int = -1
  }

  class Trident extends Arrow with MobEntity with ObjectEntity {
    @indexType(index = 1) @boxed var loyaltyLevel: Int = 0
  }

  class Boat extends Entity with MobEntity with ObjectEntity {
    @indexType(index = 1) @boxed var timeSinceLastHit: Int = 0
    @indexType(index = 1) @boxed var forwardDirection: Int = 1
    @indexType(index = 2) var damageTaken: Float = 0.0f
    @indexType(index = 1) @boxed var tpe: Int = 0
    @indexType(index = 7) var leftPaddleTurning: Boolean = false
    @indexType(index = 7) var rightPaddleTurning: Int = -1
    @indexType(index = 1) @boxed var splashTimer: Int = 0
  }

  class EnderCrystal extends Entity with MobEntity with ObjectEntity {
    @indexType(index = 10) var beamTarget: Option[Position] = None
    @indexType(index = 7) var showBottom: Boolean = true
  }

  class AbstractFireball extends Entity

  class DragonFireball extends AbstractFireball with MobEntity with ObjectEntity

  class Fireball extends AbstractFireball with MobEntity with ObjectEntity //This is the large fireball shot by ghasts.

  class SmallFireball extends AbstractFireball with MobEntity //This is the fireball shot by blazes and dispensers with fire charges.

  class WitherSkull extends AbstractFireball with MobEntity with ObjectEntity {
    @indexType(index = 7) var invulnerable: Boolean = false
  }

  class Firework extends Entity with ObjectEntity {
    @indexType(index = 6) var fireworkInfo: Slot = None
    @indexType(index = 1) @boxed var entityIdOrEntityWhichUsedFirework: Int = 0
  }

  class Hanging extends Entity

  class ItemFrame extends Hanging with MobEntity with ObjectEntity {
    @indexType(index = 6) var item: Slot = None
    @indexType(index = 1) @boxed var rotation: Int = 0
  }

  class Item extends Entity with ObjectEntity {
    @indexType(index = 6) var item: Slot = None
  }

  class Living extends Entity {
    @indexType(index = 0) @byte var handStates: Int = 0
    @indexType(index = 2) var health: Float = 1.0f
    @indexType(index = 1) @boxed var potionEffectColor: Int = 0
    @indexType(index = 7) var isPotionEffectAmbient: Boolean = false
    @indexType(index = 1) @boxed var numberOfArrowsInEntity: Int = 0
  }

  class Player extends Living with MobEntity {
    @indexType(index = 2) var additionalHealth: Float = 0.0f
    @indexType(index = 1) @boxed var score: Int = 0
    @indexType(index = 0) @byte var displayedSkinParts: Int = 0
    @indexType(index = 0) @byte var mainHand = 1 //0 left 1 right

    @indexType(index = 14) var leftShoulderEntityData : Nbt = {
      val tag: CompoundTag  = new CompoundTag()
      tag.put("ShoulderEntityLeft", new CompoundTag())
      tag
      //Nbt("", TagCompound(("ShoulderEntityLeft" , None)))
    }
    @indexType(index = 14) var rightShoulderEntityData : Nbt = {
      val tag: CompoundTag  = new CompoundTag()
      tag.put("ShoulderEntityRight", new CompoundTag())
      tag
      //Nbt("", TagCompound(("ShoulderEntityRight" , None)))
    }
  }

  class ArmorStand extends Living  with MobEntity with ObjectEntity {
    @indexType(index = 0) @byte var armorStatus: Int = 0
    @indexType(index = 8) var headRotation: Rotation = Rotation(0.0f, 0.0f, 0.0f)
    @indexType(index = 8) var bodyRotation: Rotation = Rotation(0.0f, 0.0f, 0.0f)
    @indexType(index = 8) var leftArmRotation: Rotation = Rotation(-10.0f, 0.0f, -10.0f)
    @indexType(index = 8) var rightArmRotation: Rotation = Rotation(-15.0f, 0.0f, 10.0f)
    @indexType(index = 8) var leftLegRotation: Rotation = Rotation(-1.0f, 0.0f, -1.0f)
    @indexType(index = 8) var rightLegRotation: Rotation = Rotation(1.0f, 0.0f, 1.0f)
  }

  class Insentient extends Living {
    @indexType(index = 0) @byte var insentientStatus: Int = 0
  }

  class Ambient extends Insentient

  class Bat extends Ambient with MobEntity {
    @indexType(index = 0) @byte var isHanging: Int = 0
  }

  class Creature extends  Insentient

  class WaterMob extends Creature

  class Squid extends WaterMob with MobEntity

  class Dolphin extends WaterMob with MobEntity {
    @indexType(index = 9) var treasurePosition: Position = Position(0, 0, 0)
    @indexType(index = 7) var canFindTreasure: Boolean = false
    @indexType(index = 7) var hasFish: Boolean = false
  }

  class AbstractFish extends WaterMob {
    @indexType(index = 7) var fromBucket: Boolean = false
  }

  class Cod extends AbstractFish with MobEntity

  class PufferFish extends AbstractFish with MobEntity {
    @indexType(index = 1) @boxed var puffState: Int= 0
  }

  class Salmon extends AbstractFish with MobEntity

  class TropicalFish extends AbstractFish with MobEntity {
    @indexType(index = 1) @boxed var variant: Int = 0
  }

  class Ageable extends Creature {
    @indexType(index = 7) var isBaby: Boolean = false
  }

  class Animal extends Ageable

  class AbstractHorse extends Animal {
    @indexType(index = 0) @byte var animalStatus: Int = 0
    @indexType(index = 12) var owner: Option[UUID] = None
  }
  class Horse extends AbstractHorse with MobEntity {
    @indexType(index = 1) @boxed var variant: Int = 0  //color & style
    @indexType(index = 1) @boxed var armorMaterial: Int = 0
    @indexType(index = 6) var armorItem: Slot = None // TODO: check if none need to be written
  }

  class ZombieHorse extends AbstractHorse with MobEntity

  class SkeletonHorse extends AbstractHorse with MobEntity

  class ChestedHorse extends AbstractHorse  {
    @indexType(index = 7) var hasChest: Boolean = false
  }

  class Donkey extends ChestedHorse with MobEntity

  class Llama extends ChestedHorse with MobEntity {
    @indexType(index = 1) @boxed var strength: Int = 0
    @indexType(index = 1) @boxed var carpetColor: Int = -1
    @indexType(index = 1) @boxed var variant: Int = 0
  }

  class Mule extends ChestedHorse with MobEntity

  class Pig extends Animal with MobEntity {
    @indexType(index = 7) var hasSaddle: Boolean = false
    @indexType(index = 1) @boxed var timeToBoostWithCarrotOnAStrick: Int  = 0
  }

  class Rabbit extends Animal with MobEntity {
    @indexType(index = 1) @boxed var tpe: Int = 0
  }

  class Turtle extends Animal with MobEntity {
    @indexType(index = 9) var homePosition: Position = Position(0, 0, 0)
    @indexType(index = 7) var hasEgg: Boolean = false
    @indexType(index = 7) var layingEgg: Boolean = false
    @indexType(index = 9) var travelPosition: Position = Position(0, 0, 0)
    @indexType(index  = 7) var goingHome : Boolean = false
    @indexType(index = 7) var traveling: Boolean = false
  }

  class PolarBear extends Animal with MobEntity {
    @indexType(index = 7) var standingUp: Boolean = false
  }

  class Chicken extends Animal with MobEntity

  class Cow extends Animal with MobEntity

  class Mooshroom extends Cow with MobEntity

  class Sheep extends Animal with MobEntity {
    @indexType(index = 0) @byte var sheepStatus: Int = 0
  }

  class TameableAnimal extends Animal {
    @indexType(index = 0) @byte var tameableAnimalStatus: Int = 0
    @indexType(index = 12) var owner: Option[UUID] = None
  }

  class Ocelot extends TameableAnimal with MobEntity {
    @indexType(index = 1) @boxed var tpe: Int = 0;
  }

  class Wolf extends TameableAnimal with MobEntity {
    @indexType(index = 2) var damageTaken: Float = 1.0f
    @indexType(index = 7) var isBegging: Boolean = false
    @indexType(index = 1) @boxed var collarColor: Int = 14
  }

  class Parrot extends TameableAnimal with MobEntity {
    @indexType(index = 1) @boxed var variant: Int = 0
  }

  class Villager extends Ageable with MobEntity {
    @indexType(index = 1) @boxed var profession: Int = 0
  }

  class Golem extends Creature

  class IronGolem extends Golem with MobEntity {
    @indexType(index = 0) @byte var ironGolemStatus: Int = 0
  }

  class Snowman extends Golem with MobEntity {
    @indexType(index = 0) @byte var snowmanStatus: Int = 0x10
  }

  class Shulker extends Golem with MobEntity with ObjectEntity {
    @indexType(index = 11) var attachFace: Direction = Direction.Down
    @indexType(index = 10) var attachmentPosition: Option[Position] = None
    @indexType(index = 0) @byte var shieldHeight: Int = 0
    @indexType(index = 0) @byte var color: Int = 10
  }

  class Monster extends Creature

  class Blaze extends Monster with MobEntity with ObjectEntity {
    @indexType(index = 0) @byte var blazeStatus: Int = 0
  }

  class Creeper extends Monster with MobEntity {
    @indexType(index = 1) @boxed var state: Int = -1
    @indexType(index = 7) var isCharged: Boolean = false
    @indexType(index = 7) var isIgnited: Boolean = false
  }

  class Endermite extends Monster with MobEntity

  class GiantZombie extends  Monster

  class Guardian extends Monster with MobEntity {
    @indexType(index = 7) var isRetractingSpikes: Boolean = false
    @indexType(index = 1) @boxed var targetEID: Int = 0;
  }

  class ElderGuardian extends Guardian with MobEntity

  class Silverfish extends Monster with MobEntity

  class AbstractIllager extends Monster {
    @indexType(index = 0) @byte var illagerStatus: Int = 0
  }

  class VindicationIllager extends AbstractIllager with MobEntity

  class SpellcasterIllager extends  AbstractIllager {
    @indexType(index = 0) @byte var speel: Int = 0
  }

  class EvocationIllager extends SpellcasterIllager with MobEntity

  class IllusionIllager extends SpellcasterIllager with MobEntity

  class Vex extends Monster with MobEntity {
    @indexType(index = 0) @byte var vexStatus: Int = 0
  }

  class EvocationFangs extends Entity with MobEntity with ObjectEntity

  class AbstractSkeleton extends Monster {
    @indexType(index = 7) var isSwingingArms: Boolean = false
  }

  class Skeleton extends AbstractSkeleton with MobEntity

  class WitherSkeleton extends AbstractSkeleton with MobEntity

  class Stray extends AbstractSkeleton with MobEntity

  class Spider extends Monster with MobEntity {
    @indexType(index = 0) @byte var spiderStatus: Int = 0
  }

  class Witch extends Monster with MobEntity {
    @indexType(index = 7) var witchStatus: Boolean = false
  }

  class Wither extends Monster {
    @indexType(index = 1) @boxed var centerHeadTarget: Int = 0 //entity Id or 0 if no target
    @indexType(index = 1) @boxed var leftHeadTarget: Int = 0 //entity Id or 0 if no target
    @indexType(index = 1) @boxed var rightHeadTarget: Int = 0 //entity Id or 0 if no target
    @indexType(index = 1) @boxed var invulnerableTime: Int = 0
  }

  class Zombie extends Monster with MobEntity {
    @indexType(index = 7) var isBaby: Boolean = false
    @indexType(index = 1) @boxed var unused: Int = 0
    @indexType(index = 7) var areHandsHeldUp: Boolean = false
    @indexType(index = 7) var isBecomingADrownnned: Boolean = false
  }

  class ZombieVillager extends Zombie with MobEntity {
    @indexType(index = 7) var isConverting: Boolean = false
    @indexType(index = 1) @boxed var Profession: Int = 0
  }

  class Husk extends Zombie with MobEntity

  class Drowned extends Zombie with MobEntity

  class Enderman extends  Monster with MobEntity {
    @indexType(index = 13) var carriedBlock: Option[Block] = None
    @indexType(index = 7) var isScreaming: Boolean = false
  }

  class EnderDragon extends Insentient with MobEntity {
    @indexType(index = 1) @boxed var dragonPhase: Int = 10
  }

  class Flying extends Insentient

  class Ghast extends Flying with MobEntity {
    @indexType(index = 7) var isAttacking: Boolean = false
  }

  class Phantom extends Flying with MobEntity {
    @indexType(index = 1) @boxed var size: Int = 0
  }

  class Slime extends Insentient with MobEntity {
    @indexType(index = 1)  @boxed var size: Int = 1
  }

  class LlamaSpit extends Entity with ObjectEntity

  class Minecart extends Entity with ObjectEntity {
    @indexType(index = 1) @boxed var shakingPower: Int = 0
    @indexType(index = 1) @boxed var shakingDirection: Int = 1
    @indexType(index = 2) var shakingMultiplier: Float = 0.0f
    @indexType(index = 1) @boxed var customBlockIdAndDamage: Int = 0
    @indexType(index = 1) @boxed var customBlockYposition: Int = 6
    @indexType(index = 7) var showCustomBlock: Boolean = false
  }

  class MinecartRideable extends Minecart with MobEntity

  class MinecartContainer extends Minecart

  class MinecartHopper extends MinecartContainer with MobEntity

  class MinecartChest extends MinecartContainer with MobEntity

  class MinecartFurnace extends Minecart with MobEntity {
    @indexType(index = 7) var hasFuel: Boolean = false
  }

  class MinecartTNT extends Minecart with MobEntity

  class MinecartSpawner extends Minecart with MobEntity

  class MinecartCommandBlock extends Minecart with MobEntity {
    @indexType(index = 3) var command: String = ""
    @indexType(index = 4) var lastOutput: Chat = "{'text': ''}"
  }

  class TntPrimed extends Entity with MobEntity {
    @indexType(index = 1) @boxed var fuseTime: Int = 80
  }

}
