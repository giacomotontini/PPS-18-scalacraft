package io.scalacraft.core

import java.util.UUID

import io.scalacraft.core.DataTypes.{Block, Chat, Direction, Effect, Nbt, ParticleData, Position, Rotation, Slot}
import io.scalacraft.core.PacketAnnotations._
import io.scalacraft.core.nbt.Tags.TagCompound


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
    @indexType(index = 9) val spawnPosition: Position = Position(0,0,0)
  }

  class AreaEffectCloud extends Entity with MobEntity with ObjectEntity {
    @indexType(index = 2) val radius: Float = 0.5f
    @indexType(index = 1) @boxed val color: Int = 0
    @indexType(index = 7) val ignoreRadiusAndShowEffectAtSinglePoint: Boolean = false
    @indexType(index = 15) val particle: ParticleData = Effect()
  }

  class FishingHook extends Entity with ObjectEntity {
    @indexType(index = 1) @boxed val hookedEntity: Int = 0
  }

  abstract class Arrow extends Entity with MobEntity {
    @indexType(index = 0) @byte val arrowStatus: Int = 0
    @indexType(index = 12) val shooterUUID: Option[UUID]= None
  }

  class TippedArrow extends Arrow with ObjectEntity {
    @indexType(index = 1) @boxed val color: Int = -1
  }

  class Trident extends Arrow with MobEntity with ObjectEntity {
    @indexType(index = 1) @boxed val loyaltyLevel: Int = 0
  }

  class Boat extends Entity with MobEntity with ObjectEntity {
    @indexType(index = 1) @boxed val timeSinceLastHit: Int = 0
    @indexType(index = 1) @boxed val forwardDirection: Int = 1
    @indexType(index = 2) val damageTaken: Float = 0.0f
    @indexType(index = 1) @boxed val tpe: Int = 0
    @indexType(index = 7) val leftPaddleTurning: Boolean = false
    @indexType(index = 7) val rightPaddleTurning: Int = -1
    @indexType(index = 1) @boxed val splashTimer: Int = 0
  }

  class EnderCrystal extends Entity with MobEntity with ObjectEntity {
    @indexType(index = 10) val beamTarget: Option[Position] = None
    @indexType(index = 7) val showBottom: Boolean = true
  }

  abstract class AbstractFireball extends Entity

  class DragonFireball extends AbstractFireball with MobEntity with ObjectEntity

  class Fireball extends AbstractFireball with MobEntity with ObjectEntity //This is the large fireball shot by ghasts.

  class SmallFireball extends AbstractFireball with MobEntity //This is the fireball shot by blazes and dispensers with fire charges.

  class WitherSkull extends AbstractFireball with MobEntity with ObjectEntity {
    @indexType(index = 7) val invulnerable: Boolean = false
  }

  class Firework extends Entity with ObjectEntity {
    @indexType(index = 6) val fireworkInfo: Slot = None
    @indexType(index = 1) @boxed val entityIdOrEntityWhichUsedFirework: Int = 0
  }

  class Hanging extends Entity

  class ItemFrame extends Hanging with MobEntity with ObjectEntity {
    @indexType(index = 6) val item: Slot = None
    @indexType(index = 1) @boxed val rotation: Int = 0
  }

  class Living extends Entity {
    @indexType(index = 0) @byte val handStates: Int = 0
    @indexType(index = 2) val health: Float = 1.0f
    @indexType(index = 1) @boxed val potionEffectColor: Int = 0
    @indexType(index = 7) val isPotionEffectAmbient: Boolean = false
    @indexType(index = 1) @boxed val numberOfArrowsInEntity: Int = 0
  }

  class Player extends Living with MobEntity {
    @indexType(index = 2) val additionalHealth: Float = 0.0f
    @indexType(index = 1) @boxed val score: Int = 0
    @indexType(index = 0) @byte val displayedSkinParts = 0
    @indexType(index = 0) @byte val mainHand = 1 //0 left 1 right
    @indexType(index = 14) val leftShoulderEntityData : Nbt = Nbt("", TagCompound(("ShoulderEntityLeft" , None)))
    @indexType(index = 14) val rightShoulderEntityData : Nbt = Nbt("", TagCompound(("ShoulderEntityRight" , None)))
  }

  class ArmorStand extends Living  with MobEntity with ObjectEntity {
    @indexType(index = 0) @byte val armorStatus = 0
    @indexType(index = 8) val headRotation: Rotation = Rotation(0.0f, 0.0f, 0.0f)
    @indexType(index = 8) val bodyRotation: Rotation = Rotation(0.0f, 0.0f, 0.0f)
    @indexType(index = 8) val leftArmRotation: Rotation = Rotation(-10.0f, 0.0f, -10.0f)
    @indexType(index = 8) val rightArmRotation: Rotation = Rotation(-15.0f, 0.0f, 10.0f)
    @indexType(index = 8) val leftLegRotation: Rotation = Rotation(-1.0f, 0.0f, -1.0f)
    @indexType(index = 8) val rightLegRotation: Rotation = Rotation(1.0f, 0.0f, 1.0f)
  }

  class Insentient extends Living {
    @indexType(index = 0) @byte val insentientStatus: Int = 0
  }

  class Ambient extends Insentient

  class Bat extends Ambient with MobEntity {
    @indexType(index = 0) @byte val isHanging: Int = 0
  }

  class Creature extends  Insentient

  class WaterMob extends Creature

  class Squid extends WaterMob with MobEntity

  class Dolphin extends WaterMob with MobEntity {
    @indexType(index = 9) val treasurePosition: Position = Position(0, 0, 0)
    @indexType(index = 7) val canFindTreasure: Boolean = false
    @indexType(index = 7) val hasFish: Boolean = false
  }

  class AbstractFish extends WaterMob {
    @indexType(index = 7) val fromBucket: Boolean = false
  }

  class Cod extends AbstractFish with MobEntity

  class PufferFish extends AbstractFish with MobEntity {
    @indexType(index = 1) @boxed val puffState: Int= 0
  }

  class Salmon extends AbstractFish with MobEntity

  class TropicalFish extends AbstractFish with MobEntity {
    @indexType(index = 1) @boxed val variant: Int = 0
  }

  class Ageable extends Creature {
    @indexType(index = 7) val isBaby: Boolean = false
  }

  class Animal extends Ageable

  abstract class AbstractHorse extends Animal {
    @indexType(index = 0) @byte val animalStatus: Int = 0
    @indexType(index = 12) val owner: Option[UUID] = None
  }
  class Horse extends AbstractHorse with MobEntity {
    @indexType(index = 1) @boxed val variant = 0  //color & style
    @indexType(index = 1) @boxed val armorMaterial = 0
    @indexType(index = 6) val armorItem: Slot = None
  }

  class ZombieHorse extends AbstractHorse with MobEntity

  class SkeletonHorse extends AbstractHorse with MobEntity

  class ChestedHorse extends AbstractHorse  {
    @indexType(index = 7) val hasChest: Boolean = false
  }

  class Donkey extends ChestedHorse with MobEntity

  class Llama extends ChestedHorse with MobEntity {
    @indexType(index = 1) @boxed val strength: Int = 0
    @indexType(index = 1) @boxed val carpetColor: Int = -1
    @indexType(index = 1) @boxed val variant: Int = 0
  }

  class Mule extends ChestedHorse with MobEntity

  class Pig extends Animal with MobEntity {
    @indexType(index = 7) val hasSaddle: Boolean = false
    @indexType(index = 1) @boxed val timeToBoostWithCarrotOnAStrick  = 0
  }

  class Rabbit extends Animal with MobEntity {
    @indexType(index = 1) @boxed val tpe: Int = 0
  }

  class Turtle extends Animal with MobEntity {
    @indexType(index = 9) val homePosition: Position = Position(0, 0, 0)
    @indexType(index = 7) val hasEgg: Boolean = false
    @indexType(index = 7) val layingEgg: Boolean = false
    @indexType(index = 9) val travelPosition: Position = Position(0, 0, 0)
    @indexType(index  = 7) val goingHome : Boolean = false
    @indexType(index = 7) val traveling: Boolean = false
  }

  class PolarBear extends Animal with MobEntity {
    @indexType(index = 7) val standingUp: Boolean = false
  }

  class Chicken extends Animal with MobEntity

  class Cow extends Animal with MobEntity

  class Mooshroom extends Cow with MobEntity

  class Sheep extends Animal with MobEntity {
    @indexType(index = 0) @byte val sheepStatus: Int = 0
  }

  class TameableAnimal extends Animal {
    @indexType(index = 0) @byte val tameableAnimalStatus: Int = 0
    @indexType(index = 12) val owner: Option[UUID] = None
  }

  class Ocelot extends TameableAnimal with MobEntity {
    @indexType(index = 1) @boxed val tpe: Int = 0;
  }

  class Wolf extends TameableAnimal with MobEntity {
    @indexType(index = 2) val damageTaken: Float = 1.0f
    @indexType(index = 7) val isBegging: Boolean = false
    @indexType(index = 1) @boxed val collarColor: Int = 14
  }

  class Parrot extends TameableAnimal with MobEntity {
    @indexType(index = 1) @boxed val variant: Int = 0
  }

  class Villager extends Ageable with MobEntity {
    @indexType(index = 1) @boxed val profession: Int = 0
  }

  class Golem extends Creature

  class IronGolem extends Golem with MobEntity {
    @indexType(index = 0) @byte val ironGolemStatus: Int = 0
  }

  class Snowman extends Golem with MobEntity {
    @indexType(index = 0) @byte val snowmanStatus: Int = 0x10
  }

  class Shulker extends Golem with MobEntity with ObjectEntity {
    @indexType(index = 11) val attachFace: Direction = Direction.Down
    @indexType(index = 10) val attachmentPosition: Option[Position] = None
    @indexType(index = 0) @byte val shieldHeight: Int = 0
    @indexType(index = 0) @byte val color: Int = 10
  }

  class Monster extends Creature

  class Blaze extends Monster with MobEntity with ObjectEntity {
    @indexType(index = 0) @byte val blazeStatus: Int = 0
  }

  class Creeper extends Monster with MobEntity {
    @indexType(index = 1) @boxed val state: Int = -1
    @indexType(index = 7) val isCharged: Boolean = false
    @indexType(index = 7) val isIgnited: Boolean = false
  }

  class Endermite extends Monster with MobEntity

  class GiantZombie extends  Monster

  class Guardian extends Monster with MobEntity {
    @indexType(index = 7) val isRetractingSpikes: Boolean = false
    @indexType(index = 1) @boxed val targetEID = false;
  }

  class ElderGuardian extends Guardian with MobEntity

  class Silverfish extends Monster with MobEntity

  abstract class AbstractIllager extends Monster {
    @indexType(index = 0) @byte val illagerStatus: Int = 0
  }

  class VindicationIllager extends AbstractIllager with MobEntity

  class SpellcasterIllager extends  AbstractIllager {
    @indexType(index = 0) @byte val speel: Int = 0
  }

  class EvocationIllager extends SpellcasterIllager with MobEntity

  class IllusionIllager extends SpellcasterIllager with MobEntity

  class Vex extends Monster with MobEntity {
    @indexType(index = 0) @byte val vexStatus: Int = 0
  }

  class EvocationFangs extends Entity with MobEntity with ObjectEntity

  abstract class AbstractSkeleton extends Monster {
    @indexType(index = 7) val isSwingingArms: Boolean = false
  }

  class Skeleton extends AbstractSkeleton with MobEntity

  class WitherSkeleton extends AbstractSkeleton with MobEntity

  class Stray extends AbstractSkeleton with MobEntity

  class Spider extends Monster with MobEntity {
    @indexType(index = 0) @byte val spiderStatus: Int = 0
  }

  class Witch extends Monster with MobEntity {
    @indexType(index = 7) val witchStatus: Boolean = false
  }

  class Wither extends Monster {
    @indexType(index = 1) @boxed val centerHeadTarget = 0 //entity Id or 0 if no target
    @indexType(index = 1) @boxed val leftHeadTarget = 0 //entity Id or 0 if no target
    @indexType(index = 1) @boxed val rightHeadTarget = 0 //entity Id or 0 if no target
    @indexType(index = 1) @boxed val invulnerableTime = 0
  }

  class Zombie extends Monster with MobEntity {
    @indexType(index = 7) val isBaby: Boolean = false
    @indexType(index = 1) @boxed val unused: Int = 0
    @indexType(index = 7) val areHandsHeldUp: Boolean = false
    @indexType(index = 7) val isBecomingADrownnned: Boolean = false
  }

  class ZombieVillager extends Zombie with MobEntity {
    @indexType(index = 7) val isConverting: Boolean = false
    @indexType(index = 1) @boxed val Profession = false
  }

  class Husk extends Zombie with MobEntity

  class Drowned extends Zombie with MobEntity

  class Enderman extends  Monster with MobEntity {
    @indexType(index = 13) val carriedBlock: Option[Block] = None
    @indexType(index = 7) val isScreaming: Boolean = false
  }

  class EnderDragon extends Insentient with MobEntity {
    @indexType(index = 1) @boxed val dragonPhase: Int = 10
  }

  class Flying extends Insentient

  class Ghast extends Flying with MobEntity {
    @indexType(index = 7) val isAttacking: Boolean = false
  }

  class Phantom extends Flying with MobEntity {
    @indexType(index = 1) @boxed val size: Int = 0
  }

  class Slime extends Insentient with MobEntity {
    @indexType(index = 1)  @boxed val size: Int = 1
  }

  class LlamaSpit extends Entity with ObjectEntity

  class Minecart extends Entity with ObjectEntity {
    @indexType(index = 1) @boxed val shakingPower:Int = 0
    @indexType(index = 1) @boxed val shakingDirection:Int = 1
    @indexType(index = 2) val shakingMultiplier:Float = 0.0f
    @indexType(index = 1) @boxed val customBlockIdAndDamage:Int = 0
    @indexType(index = 1) @boxed val customBlockYposition:Int = 6
    @indexType(index = 7) val showCustomBlock: Boolean = false
  }

  class MinecartRideable extends Minecart with MobEntity

  class MinecartContainer extends Minecart

  class MinecartHopper extends MinecartContainer with MobEntity

  class MinecartChest extends MinecartContainer with MobEntity

  class MinecartFurnace extends Minecart with MobEntity {
    @indexType(index = 7) val hasFuel: Boolean = false
  }

  class MinecartTNT extends Minecart with MobEntity

  class MinecartSpawner extends Minecart with MobEntity

  class MinecartCommandBlock extends Minecart with MobEntity {
    @indexType(index = 3) val command: String = ""
    @indexType(index = 4) val lastOutput: Chat = "{'text': ''}"
  }

  class TntPrimed extends Entity with MobEntity {
    @indexType(index = 1) @boxed val fuseTime: Int = 80
  }


}
