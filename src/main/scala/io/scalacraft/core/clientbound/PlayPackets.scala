package io.scalacraft.core.clientbound

import java.util.Locale.Category
import java.util.UUID

import io.scalacraft.core.DataTypes.{Angle, Chat, Direction, Identifier, Nbt, Position, Slot, VarInt}
import io.scalacraft.core.Entities.{Entity, MobEntity, Player}
import io.scalacraft.core.PacketAnnotations.{short, _}
import io.scalacraft.core.Structure

object PlayPackets {

  @packet(id = 0x00)
  case class SpawnObject(@boxed entityId: Int,
                         objectUUID: UUID,
                         @byte tpe: Int,
                         x: Double,
                         y: Double,
                         z: Double,
                         pitch: Angle,
                         yaw: Angle,
                         data: Int,
                         @short velocityX: Int,
                         @short velocityY: Int,
                         @short velocityZ: Int) extends Structure

  @packet(id = 0x01)
  case class SpawnExperienceOrb(@boxed entityId: Int,
                                x: Double,
                                y: Double,
                                z: Double,
                                @short count: Int) extends Structure


  sealed trait GlobalEntityType

  case object GlobalEntityType {

    @enumValue(1) case object Thunderbolt extends GlobalEntityType

  }

  @packet(id = 0x02)
  case class SpawnGlobalEntity(@boxed entityId: Int,
                               @enumType[Byte] tpe: Int,
                               x: Double,
                               y: Double,
                               z: Double) extends Structure

  @packet(id = 0x03)
  case class SpawnMob(@boxed entityId: Int,
                      entityUUID: UUID,
                      @boxed tpe: Int,
                      x: Double,
                      y: Double,
                      z: Double,
                      yaw: Angle,
                      pitch: Angle,
                      headPitch: Angle,
                      velocityX: Short,
                      velocityY: Short,
                      velocityZ: Short,
                      @fromContext(2) metadata: MobEntity) extends Structure

  @packet(id = 0x04)
  case class SpawnPainting(@boxed entityId: Int,
                           entityUUID: UUID,
                           @boxed motive: Int,
                           location: Position, //center coordinate, see wiki for computer image centering
                           @enumType[Byte] direction: Direction) extends Structure

  @packet(id = 0x05)
  case class SpawnPlayer(@boxed entityId: Int,
                         playerUUID: UUID,
                         x: Double,
                         y: Double,
                         z: Double,
                         yaw: Angle,
                         pitch: Angle,
                         metadata: Player) extends Structure //TODO: we do not have type, should be handled

  sealed trait AnimationType

  case object AnimationType {

    @enumValue(value = 0) case object SwingMainArm extends AnimationType

    @enumValue(value = 1) case object TakeDamage extends AnimationType

    @enumValue(value = 2) case object LeaveBed extends AnimationType

    @enumValue(value = 3) case object SwingOffHand extends AnimationType

    @enumValue(value = 4) case object CriticalEffect extends AnimationType

    @enumValue(value = 5) case object MagicCriticalEffect extends AnimationType

  }

  @packet(id = 0x06)
  case class Animation(@boxed entityId: Int,
                       @enumType[Byte] animation: Animation)  extends Structure

  case class CategoryStatistic(@enumType[VarInt] categoryId: CategoriesType,
                               @boxed statisticId: Int) extends Structure

  sealed trait CategoriesType

  case object CategoriesType {

    @enumValue(value = 0) case object Mined extends CategoriesType

    @enumValue(value = 1) case object Crafted extends CategoriesType

    @enumValue(value = 2) case object Used extends CategoriesType

    @enumValue(value = 3) case object Broken extends CategoriesType

    @enumValue(value = 4) case object PickedUp extends CategoriesType

    @enumValue(value = 5) case object Dropped extends CategoriesType

    @enumValue(value = 6) case object Killed extends CategoriesType

    @enumValue(value = 7) case object KilledBy extends CategoriesType
    @enumValue(value = 8) case object Custom extends CategoriesType
  }

  @packet(id = 0x07)
  case class Statistics(@boxed count: Int,
                        @precededBy[VarInt] statistic: List[CategoryStatistic],
                        @boxed value: Int) extends Structure


  @packet(id = 0x08)
  case class BlockBreakAnimation(@boxed entityId: Int,
                                 location: Position,
                                @byte destroyStage: Int) extends Structure //0-9 normal-bad stage, > 9 destroyed

  @packet(id = 0x09)
  case class UpdateBlockEntity(location: Position, action: Int, nbtData: Nbt) extends Structure //TODO: see protocol to understabd action data meaning

  @packet(id = 0x0A)
  case class BlockAction(location: Position,
                         @byte actionId: Int,
                         @byte actionParam: Int,
                         @boxed blockType: Int) extends Structure

  @packet(id = 0x0B)
  case class BlockChange(location: Position,
                         @boxed blockId: Int) extends Structure

  sealed trait BossBarColor

  case object BossBarColor {

    @enumValue(0) case object Pink extends BossBarColor

    @enumValue(1) case object Blue extends BossBarColor

    @enumValue(2) case object Red extends BossBarColor

    @enumValue(3) case object Green extends BossBarColor

    @enumValue(4) case object Yellow extends BossBarColor

    @enumValue(5) case object Purple extends BossBarColor

    @enumValue(6) case object White extends BossBarColor

  }

  sealed trait BossBarDivision

  case object BossBarDivision {

    @enumValue(0) case object NoDivision extends BossBarDivision

    @enumValue(1) case object SixNotches extends BossBarDivision

    @enumValue(2) case object TenNotches extends BossBarDivision

    @enumValue(3) case object TwelveNotches extends BossBarDivision

    @enumValue(4) case object Twenty extends BossBarDivision

  }

  sealed trait BossBarAction

  @switchKey(0)
  case class BossBarAddAction(title: Chat,
                              health: Float,
                              @enumType[BossBarColor] @boxed color: Int,
                              @enumType[BossBarDivision] @boxed division: Int,
                              @byte flags: Int) extends BossBarAction

  @switchKey(1)
  case class BossBarRemoveAction() extends BossBarAction

  @switchKey(2)
  case class BossBarHealthAction(health: Float) extends BossBarAction

  @switchKey(3)
  case class BossBarUpdateTitleAction(title: Chat) extends BossBarAction

  @switchKey(4)
  case class BossBarUpdateStyleAction(@enumType[BossBarColor] @boxed color: Int,
                                      @enumType[BossBarDivision] @boxed dividers: Int) extends BossBarAction

  @switchKey(4)
  case class BossBarUpdateFlagsAction(@byte flags: Int) extends BossBarAction

  @packet(id = 0x0C)
  case class BossBar(uuid: UUID, @boxed action: Int, @fromContext(1) @switchType[VarInt] body: BossBarAction)

  sealed trait ServerDifficulties

  case object ServerDifficulties {

    @enumValue(0) case object Peaceful extends ServerDifficulties

    @enumValue(1) case object Easy extends ServerDifficulties

    @enumValue(2) case object Normal extends ServerDifficulties

    @enumValue(3) case object Hard extends ServerDifficulties

  }

  @packet(id = 0x0D)
  case class ServerDifficulty(@enumType[Byte] @byte difficulty: ServerDifficulties) extends Structure

  sealed trait ChatPosition

  case object ChatPosition {

    @enumValue(0) case object ChatBox extends ChatPosition

    @enumValue(1) case object SystemMessage extends ChatPosition

    @enumValue(2) case object GameInfo extends ChatPosition

  }

  @packet(id = 0x0E)
  case class ChatMessage(jsonData: Chat, @enumType[Byte] position: ChatPosition) extends Structure

  case class MultiBlockChangeRecord(@byte horizontalPosition: Int,
                                    @byte yCoordinate: Int,
                                    @boxed blockId: Int) extends Structure

  @packet(id = 0x0F)
  case class MultiBlockChange(chunkX: Int,
                              chunkY: Int,
                              @boxed recordCount: Int,
                              @precededBy[VarInt] record: List[MultiBlockChangeRecord]) extends Structure //check protocol for position decoding algorithm

  case class TabCompleteMatches(tabMatch: String,
                                tooltip: Option[Chat]) extends Structure

  @packet(id = 0x10)
  case class TabComplete(@boxed id: Int,
                         @boxed start: Int,
                         @boxed length: Int,
                         @boxed count: Int,
                         @precededBy[VarInt] matches: List[TabCompleteMatches]) extends Structure

  /* TODO: support NODE data type
  @packet(id = 0x11)
  case class DeclareCommands(@boxed count: Int,
                             @precededBy[VarInt] nodes: List[Node],
                             @boxed rootIndex: Int) extends Structure
   */

  @packet(id = 0x12)
  case class ConfirmTransaction(@byte windowsId: Int,
                                @short actionNumber: Int,
                                accepted: Boolean) extends Structure

  @packet(id = 0x13)
  case class CloseWindows(@byte windowId: Int) extends Structure

  sealed trait WindowType

  case object WindowsType {

    @enumValue("minecraft:container") case object Container extends WindowType

    @enumValue("minecraft:chest") case object Chest extends WindowType

    @enumValue("minecraft:crafting_table") case object CraftingTable extends WindowType

    @enumValue("minecraft:furnace") case object Furnace extends WindowType

    @enumValue("minecraft:dispenser") case object Dispenser extends WindowType

    @enumValue("minecraft:enchanting_table") case object EnchantingTable extends WindowType

    @enumValue("minecraft:brewing_stand") case object BrewingStand extends WindowType

    @enumValue("minecraft:villager") case object Villager extends WindowType

    @enumValue("minecraft:beacon") case object Beacon extends WindowType

    @enumValue("minecraft:anvil") case object Anvil extends WindowType

    @enumValue("minecraft:hopper") case object Hopper extends WindowType

    @enumValue("minecraft:dropper") case object Dropper extends WindowType

    @enumValue("minecraft:shulker_box") case object ShulkerBox extends WindowType

    @enumValue("EntityHorse") case object EntityHorse extends WindowType

  }

  @packet(id = 0x14)
  case class OpenWindows(@byte windowId: Int,
                         @enumType[String] windowType: WindowType,
                         windowTitle: Chat,
                         @byte numberOfSlots: Int,
                         @fromContext(2) entityId: Option[Int]) extends Structure //TODO: the option presence is determined by a string value => how we handle that?

  @packet(id = 0x15)
  case class WindowItems(@byte windowId: Int, @short count: Int, @precededBy[Short] slot: List[Slot]) extends Structure

  //TODO: 0x16 Packet unclear

  @packet(id = 0x17)
  case class SetSlot(@byte windowId: Int,
                     slot: Short, //the slot that should be updated
                     slotData: Slot) extends Structure

  @packet(id = 0x18)
  case class SetCooldown(@boxed itemId: Int, @boxed cooldownTicks: Int) extends Structure

  @packet(id = 0x19)
  case class PluginMessage(channel: Identifier, @byte data: List[Int]) extends Structure

  sealed trait SoundCategory

  case object SoundCategory {

    @enumValue(0) case object Master extends SoundCategory

    @enumValue(1) case object Music extends SoundCategory

    @enumValue(2) case object Records extends SoundCategory

    @enumValue(3) case object Weather extends SoundCategory

    @enumValue(4) case object Blocks extends SoundCategory

    @enumValue(5) case object Hostile extends SoundCategory

    @enumValue(6) case object Neutral extends SoundCategory

    @enumValue(7) case object Players extends SoundCategory

    @enumValue(8) case object Ambient extends SoundCategory

    @enumValue(9) case object Voice extends SoundCategory

  }

  @packet(id = 0x1A)
  case class NamedSoundEffect(soundName: Identifier,
                              @enumType[VarInt] soundCategory: SoundCategory,
                              effectPositionX: Int,
                              effectPositionY: Int,
                              effectPositionZ: Int,
                              volume: Float,
                              pitch: Float) extends Structure
  @packet(id = 0x1B)
  case class Disconnect(reason: Chat) extends Structure

  sealed trait EntityStatusEnum

  case object EntityStatusEnum {

    @enumValue(1) case object p1 extends EntityStatusEnum

    //TODO: need to be handled
  }

  @packet(id = 0x1C)
  case class EntityStatus(entityId: Int, @enumType[Byte] entityStatus: EntityStatusEnum) extends Structure

  @packet(id = 0x1D)
  case class NbtQueryResponse(@boxed transactionId: Int, nbt: Nbt) extends Structure

  case class ExplosionOffset(x: Long, y: Long, z: Long) extends Structure

  @packet(id = 0x1E)
  case class Explosion(x: Float,
                       y: Float,
                       z: Float,
                       radius: Float,
                       recordCount: Int,
                       @precededBy[Int] records: List[ExplosionOffset],
                       playerMotionX: Float,
                       playerMotionY: Float,
                       playerMotionZ: Float) extends Structure

  @packet(id = 0x1F)
  case class UnloadChunk(chunkX : Int, chunkY : Int) extends Structure // Block coordinate divided by 16, rounded down


  sealed trait GameModeValue

  case object GameMode {

    @enumValue(0.0f) case object Survival extends GameModeValue

    @enumValue(1.0f) case object Creative extends GameModeValue

    @enumValue(2.0f) case object Adventure extends GameModeValue
    @enumValue(3.0f) case object Spectator extends GameModeValue
  }

  sealed trait ExitModeValue

  case object ExitMode {

    @enumValue(0.0f) case object RespawnWithoutShowAndCredits extends ExitModeValue

    @enumValue(1.0f) case object ShowEndCreditsAndRespawn extends ExitModeValue

  }

  sealed trait DemoMessageValue

  case object DemoMessage {

    @enumValue(0.0f) case object ShowWelcomeToDemoScreen extends DemoMessageValue

    @enumValue(101.0f) case object TellMovementControls extends DemoMessageValue

    @enumValue(102.0f) case object TellJumpControl extends DemoMessageValue

    @enumValue(103.0f) case object TellInventoryControl extends DemoMessageValue

  }

  sealed trait FadeValue

  case object FadeValue {

    @enumValue(0.0f) case object Dark extends FadeValue

    @enumValue(1.0f) case object Bright extends FadeValue

  }


  sealed trait ReasonCode

  case object ReasonCode {

    @enumValue(0) case class InvalidBed(value: Float) extends ReasonCode

    @enumValue(1) case class EndRaining(value: Float) extends ReasonCode

    @enumValue(2) case class BeginRaining(value: Float) extends ReasonCode

    @enumValue(3) case class ChangeGameMode(@enumType[Float] value: GameModeValue) extends ReasonCode

    @enumValue(4) case class ExitEnd(@enumType[Float] value: ExitModeValue) extends ReasonCode

    @enumValue(5) case class DemoMessage(@enumType[Float] value: DemoMessage) extends ReasonCode

    @enumValue(6) case class ArrowHittingPlayer(value: Float) extends ReasonCode

    @enumValue(7) case class FadeValues(@enumType[Float] value: FadeValue) extends ReasonCode

    @enumValue(8) case class FadeTime(value: Float) extends ReasonCode

    @enumValue(9) case class PlayPufferFishStingSound(value: Float) extends ReasonCode

    @enumValue(value = 10) case class PlayElderGuardianMob(value: Float) extends ReasonCode

  }

  @packet(id = 0x20)
  case class ChangeGameState(@enumType[Byte] @byte reason: Int, @fromContext(0) @switchType[Byte]  body: ReasonCode )  extends Structure

  @packet(id= 0x21)

  @packet(id = 0x22)
  case class ChunkData(chunkX: Int,
                       chunkZ: Int,
                       fullChunk: Boolean,
                       @boxed primaryBitMask: Int,
                       @boxed size: Int,
                       @precededBy[VarInt] @boxed data: List[Int],
                       @boxed numberOfBlockEntities: Int,
                       @precededBy[VarInt] blockEntities: List[Nbt]) extends Structure

  sealed trait EffectId

  case object EffectId {

    //Sound Effects
    @enumValue(1000) case object DispenserDispenses extends EffectId

    @enumValue(1001) case object DispenserFailsToDispense extends EffectId

    @enumValue(1002) case object DispenserShoots extends EffectId

    @enumValue(1003) case object EndeEyeLaunched extends EffectId

    @enumValue(1004) case object FireworkShot extends EffectId

    @enumValue(1005) case object IronDoorOpened extends EffectId

    @enumValue(1006) case object WoodenDoorOpened extends EffectId

    @enumValue(1007) case object WoodenTrapdoorOpened extends EffectId

    @enumValue(1008) case object FenceGateOpened extends EffectId

    @enumValue(1009) case object FireExtinguished extends EffectId

    @enumValue(1010) case object PlayRecord extends EffectId //data binded to this id specifies sound direction
    @enumValue(1011) case object IronDooClosed extends EffectId

    @enumValue(1012) case object WoodenDoorClosed extends EffectId

    @enumValue(1013) case object WoodenTrapdoorClosed extends EffectId

    @enumValue(1014) case object FenceGateClosed extends EffectId

    @enumValue(1015) case object GhastWarns extends EffectId

    @enumValue(1016) case object GhastShoots extends EffectId

    @enumValue(1017) case object EnderdragonShoots extends EffectId

    @enumValue(1018) case object BlazeShoots extends EffectId

    @enumValue(1019) case object ZombieAttacksWoodDoor extends EffectId

    @enumValue(1020) case object ZombieAttacksIronDoor extends EffectId

    @enumValue(1021) case object ZombieBreaksWoodDoor extends EffectId

    @enumValue(1022) case object WitherBreaksBlock extends EffectId

    @enumValue(1023) case object WitherSpawned extends EffectId

    @enumValue(1024) case object WitherShoots extends EffectId

    @enumValue(1025) case object BatTakesOff extends EffectId

    @enumValue(1026) case object ZombieInfects extends EffectId

    @enumValue(1027) case object ZombiVillagerConverted extends EffectId

    @enumValue(1028) case object EnderDragonDeath extends EffectId

    @enumValue(1029) case object AnvilDestroyed extends EffectId

    @enumValue(1030) case object AnvilUsed extends EffectId

    @enumValue(1031) case object AnvilLanded extends EffectId

    @enumValue(1032) case object PortalTravel extends EffectId

    @enumValue(1033) case object ChorusFlowerGrown extends EffectId

    @enumValue(1034) case object ChorusFlowerDied extends EffectId

    @enumValue(1035) case object BrewingStandBrewed extends EffectId

    @enumValue(1036) case object IroTtrapdoorOpened extends EffectId

    @enumValue(1037) case object IronTrapdoorClosed extends EffectId

    // Particles Effects
    @enumValue(2000) case object SpawnsTenSmokeParticles

    @enumValue(2001) case object BlockBreakWithSound

    @enumValue(2002) case object SplashPotion

    @enumValue(2003) case object EyeOfEnderEntityBreakAnimation

    @enumValue(2004) case object MobSpawnParticleSmokeAndFlames

    @enumValue(2005) case object BonemealParticles

    @enumValue(2006) case object DragonBreath

    @enumValue(2007) case object InstantSplashPotion

    @enumValue(3000) case object EndGatewaySpawn

    @enumValue(3001) case object EnderdragonGrowl

  }

  @packet(id = 0x23)
  case class Effect(@enumType[Int] effectId: EffectId,
                    location: Position,
                    data: Int,
                    disableRelativeVolume: Boolean) extends Structure

  sealed trait ParticleStructure

  @switchKey(0) case class AmbientEntityEffect() extends ParticleStructure

  @switchKey(1) case class AngryVillager() extends ParticleStructure

  @switchKey(2) case class Barrier() extends ParticleStructure

  @switchKey(3) case class Block() extends ParticleStructure

  @switchKey(4) case class Bubble() extends ParticleStructure

  @switchKey(5) case class Cloud() extends ParticleStructure

  @switchKey(6) case class Crit() extends ParticleStructure

  @switchKey(7) case class DamageIndicator() extends ParticleStructure

  @switchKey(8) case class DragonBreath() extends ParticleStructure

  @switchKey(9) case class DrippingLava() extends ParticleStructure

  @switchKey(10) case class DrippingWater() extends ParticleStructure

  @switchKey(11) case class Dust(red: Float,
                                 green: Float,
                                 blue: Float,
                                 scale: Float) extends ParticleStructure

  @switchKey(12) case class EffectParticle() extends ParticleStructure

  @switchKey(13) case class ElderGuardian() extends ParticleStructure

  @switchKey(14) case class EnchantedHit() extends ParticleStructure

  @switchKey(15) case class Enchant() extends ParticleStructure

  @switchKey(16) case class EndRod() extends ParticleStructure

  @switchKey(17) case class EntityEffectParticle() extends ParticleStructure

  @switchKey(18) case class ExplosionEmitter() extends ParticleStructure

  @switchKey(19) case class ExplosionParticle() extends ParticleStructure

  @switchKey(20) case class FallingDust(@boxed blockState: Int) extends ParticleStructure

  @switchKey(21) case class Firework() extends ParticleStructure

  @switchKey(22) case class Fishing() extends ParticleStructure

  @switchKey(23) case class Flame() extends ParticleStructure

  @switchKey(24) case class HappyVillager() extends ParticleStructure

  @switchKey(25) case class Heart() extends ParticleStructure

  @switchKey(26) case class InstantEffect() extends ParticleStructure

  @switchKey(27) case class Item(item: Slot) extends ParticleStructure

  @switchKey(28) case class ItemSlime() extends ParticleStructure

  @switchKey(29) case class ItemSnowball() extends ParticleStructure

  @switchKey(30) case class LargeSmoke() extends ParticleStructure

  @switchKey(31) case class Lava() extends ParticleStructure

  @switchKey(32) case class Mycelium() extends ParticleStructure

  @switchKey(33) case class Note() extends ParticleStructure

  @switchKey(34) case class Poof() extends ParticleStructure

  @switchKey(35) case class Portal() extends ParticleStructure

  @switchKey(36) case class Rain() extends ParticleStructure

  @switchKey(37) case class Smoke() extends ParticleStructure

  @switchKey(38) case class Spit() extends ParticleStructure

  @switchKey(39) case class SquidInk() extends ParticleStructure

  @switchKey(40) case class SweepAttack() extends ParticleStructure

  @switchKey(41) case class TotemOfUndying() extends ParticleStructure

  @switchKey(42) case class Underwater() extends ParticleStructure

  @switchKey(43) case class Splash() extends ParticleStructure

  @switchKey(44) case class Witch() extends ParticleStructure

  @switchKey(45) case class BubblePop() extends ParticleStructure

  @switchKey(46) case class CurrentDown() extends ParticleStructure

  @switchKey(47) case class BubbleColumnUp() extends ParticleStructure

  @switchKey(48) case class Nautilus() extends ParticleStructure

  @switchKey(49) case class Dolphin() extends ParticleStructure

  @packet(id = 0x24)
  case class Particle(particleId: Int,
                      longDistance: Boolean,
                      x: Float,
                      y: Float,
                      z: Float,
                      offsetX: Float,
                      offsetY: Float,
                      offsetZ: Float,
                      particleData: Float,
                      particleCount: Int,
                      @switchType[Int] @fromContext(0) data: ParticleStructure) extends Structure


  sealed trait WorldDimension
  case object WordDimension {
    @enumValue(-1) case class Nether() extends WorldDimension
    @enumValue(0) case class Overworld() extends WorldDimension
    @enumValue(1) case class End() extends WorldDimension
  }

  sealed trait LevelType
  case object LevelType {
    @enumValue("default") case class Default() extends LevelType
    @enumValue("flat") case class Flat() extends LevelType
    @enumValue("largeBiomes") case class LargeBiomes() extends LevelType
    @enumValue("amplified") case class Amplified() extends LevelType
    @enumValue("custom") case class Custom() extends LevelType
    @enumValue("buffet") case class Buffet() extends LevelType
  }

  @packet(id = 0x25)
  case class JoinGame(entityId: Int,
                      gameMode: Byte,
                      @enumType[Int] dimension: WorldDimension,
                      difficulty: ServerDifficulties,
                      @byte maxPlayers: Int,
                      @enumType[String] levelType: LevelType,
                      reducedDebugInfo: Boolean) extends Structure

  case class Icon(@byte x: Int,
                  @byte z: Int,
                  @byte direction: Int,
                  //hasDisplayName: Boolean,
                  displayName: Option[Chat]
                 ) extends Structure

  @packet(id = 0x26)
  case class MapData(@boxed mapId: Int,
                     @byte scale: Int,
                     trackingPosition: Boolean,
                     @boxed iconCount: Int,
                     @precededBy[VarInt] icons: List[Icon],
                     @byte columns: Int,
                     @fromContext(5) @byte row: Option[Int],
                     @fromContext(5) @byte x: Option[Int],
                     @fromContext(5) @byte z: Option[Int],
                     @fromContext(5) @boxed length: Option[Int],
                     @fromContext(5) @byte data: List[Int] //TODO check
                    ) extends Structure

  @packet(id = 0x27)
  case class Entity(@boxed entityId: Int) extends Structure

  @packet(id = 0x28)
  case class EntityRelativeMove(@boxed entityId: Int,
                                @short deltaX: Int,
                                @short deltaY: Int,
                                @short deltaZ: Int,
                                onGround: Boolean)  extends Structure

  @packet(id = 0x29)
  case class EntityLockAndRelativeMove(@boxed entityId: Int,
                                       @short deltaX: Int,
                                       @short deltaY: Int,
                                       @short deltaZ: Int,
                                       yaw: Angle,
                                       pitch: Angle,
                                       onGround: Boolean) extends Structure

  @packet(id = 0x2A)
  case class EntityLook(@boxed entityId: Int,
                        yaw: Angle,
                        pitch: Angle,
                        onGround: Boolean) extends Structure

  @packet(id = 0x2B)
  case class VehicleMove(x: Double,
                         y: Double,
                         z: Double,
                         yaw: Float,
                         pitch: Float) extends Structure

  @packet(id = 0x2C)
  case class OpenSignEditor(location: Position) extends Structure

  @packet(id = 0x2D)
  case class CraftRecipeResponse(@byte windowId: Int, recipe: Identifier) extends Structure

  @packet(id = 0x2E)
  case class PlayerAbilities(@byte flags: Int,
                             flyingSpeed: Float = 0.05f,
                             fieldOfViewModifier: Float = 0.1f) extends Structure

  sealed trait CombatEventType
  @switchKey(0) case class CombatEventEnterCombat() extends CombatEventType
  @switchKey(1) case class CombatEventEndCombat(@boxed duration: Int,
                                  entityId: Int) extends CombatEventType
  @switchKey(2) case class CombatEventEntityDead(@boxed playerId: Int,
                                   entityId: Int,
                                   message: Chat) extends CombatEventType

  @packet(id = 0x2F)
  case class CombatEvent(@boxed event: Int,
                         @fromContext(0) @switchType[VarInt] body: CombatEventType) extends Structure

  sealed trait TeamColor
  object TeamColor {
    @enumValue(0) case object Black extends TeamColor
    @enumValue(1) case object DarkBlue extends TeamColor
    @enumValue(2) case object DarkGreen extends TeamColor
    @enumValue(3) case object DarkCyan extends TeamColor
    @enumValue(4) case object DarkRed extends TeamColor
    @enumValue(5) case object Purple extends TeamColor
    @enumValue(6) case object Gold extends TeamColor
    @enumValue(7) case object Gray extends TeamColor
    @enumValue(8) case object DarkGray extends TeamColor
    @enumValue(9) case object Blue extends TeamColor
    @enumValue(10) case object BrightGreen extends TeamColor
    @enumValue(11) case object Cyan extends TeamColor
    @enumValue(12) case object Red extends TeamColor
    @enumValue(13) case object Pink extends TeamColor
    @enumValue(14) case object Yellow extends TeamColor
    @enumValue(15) case object White extends TeamColor
    @enumValue(16) case object Obfuscated extends TeamColor
    @enumValue(17) case object Bold extends TeamColor
    @enumValue(18) case object Strikethrough extends TeamColor
    @enumValue(19) case object Underlined extends TeamColor
    @enumValue(20) case object Itelic extends TeamColor
    @enumValue(21) case object Reset extends TeamColor

  }
  sealed trait NameTagVisibility
  object NameTagVisibility {
    @enumValue("always") case object Always extends NameTagVisibility
    @enumValue("hideForOtherTeams") case object HideForOtherTeams extends NameTagVisibility
    @enumValue("hideForOwnTeam") case object HideForOwnTeam extends NameTagVisibility
    @enumValue("never") case object Never extends NameTagVisibility
  }
  sealed trait CollisionRule
  object CollectRule {
    @enumValue("always") case object Always extends NameTagVisibility
    @enumValue("pushOtherTeams") case object PushOtherTeams extends NameTagVisibility
    @enumValue("pushOwnTeam") case object PushOwnTeam extends NameTagVisibility
    @enumValue("never") case object Never extends NameTagVisibility
  }

  case class TeamInfo(teamDisplayName: Chat,
                      @byte friendlyFlags: Int,
                      @enumType[String] nameTagVisibility: NameTagVisibility,
                      @enumType[String] collisionRule: CollisionRule,
                      @enumType[VarInt] teamColor: TeamColor,
                      teamPrefix: Chat,
                      teamSuffix: Chat) extends Structure
  sealed trait ModePacket47
  @switchKey(0) case class CreateTeam(info: TeamInfo,
                                      @precededBy[VarInt] @maxLength(40) entities: List[String]
                                     ) extends ModePacket47

  @switchKey(1) case class RemoveTeam() extends ModePacket47

  @switchKey(2) case class UpdateTeamInfo(info: TeamInfo) extends ModePacket47

  @switchKey(3) case class AddPlayersToTeam(@precededBy[VarInt] @maxLength(40) entities: List[String]) extends ModePacket47

  @switchKey(4) case class RemovePlayersFromTeam(@precededBy[VarInt] @maxLength(40) entities: List[String]) extends ModePacket47

  @packet(0x47)
  case class Teams(@maxLength(16) teamName: String, @switchType[Byte] mode: ModePacket47) extends Structure

  //TODO: check
  @packet(0x48)
  case class UpdateScore(@maxLength(40) entityName: String,
                         @byte action: Int,
                         @maxLength(16) objectiveName: String,
                         @fromContext(1) value: Option[VarInt]) extends Structure
  @packet(0x49)
  case class SpawnPosition(location: Position) extends Structure

  @packet(0x4A)
  case class TimeUpdate(worldAge: Long, timeOfDay: Long) extends Structure

  sealed trait ActionPacket4B

  @switchKey(0) case class SetTitle(titleText: Chat) extends ActionPacket4B

  @switchKey(1) case class SetSubtitle(subtitleText: Chat) extends ActionPacket4B

  @switchKey(2) case class SetActionbar(actionBarText: Chat) extends ActionPacket4B

  @switchKey(3) case class SetTimesAndDisplay(fadeIn: Int, stay: Int, fadeOut: Int) extends ActionPacket4B

  @switchKey(4) case class SetHide() extends ActionPacket4B

  @switchKey(5) case class SetReset() extends ActionPacket4B

  @packet(0x4B)
  case class Title(@switchType[VarInt] action: ActionPacket4B) extends Structure

  @packet(0x4C)
  case class StopSound(flags: Byte,
                       @enumType[VarInt] source: SoundCategory,
                       sound: Option[Identifier]) extends Structure

  @packet(0x4D)
  case class SoundEffect(@boxed soundId: Int,
                         @enumType[VarInt] soundCategory: SoundCategory,
                         effectPositionX: Int,
                         effectPositionY: Int,
                         effectPositionZ: Int,
                         volume: Float, pitch: Float) extends Structure

  @packet(0x4E)
  case class PlayerListHeaderAndFooter(header: Chat,
                                       footer: Chat) extends Structure

  @packet(0x4F)
  case class CollectItem(@boxed collectEntityId: Int,
                         @boxed collectorEntityId: Int,
                         @boxed pickUpItemCount: Int) extends Structure

  @packet(0x50)
  case class EntityTeleport(@boxed entityId: Int,
                            x: Double, y: Double,
                            z: Double, yaw: Angle,
                            pitch: Angle,
                            onGround: Boolean) extends Structure

  sealed trait FrameType

  object FrameType {

    @enumValue(0) case object Task extends FrameType

    @enumValue(1) case object Challenge extends FrameType

    @enumValue(2) case object Goal extends FrameType

  }

  //TODO: Check void meaning in the packet structures
  case class Criteria(key: Identifier, value: Void) extends Structure

  case class Requirement(@precededBy[VarInt] requirement: List[String]) extends Structure

  case class AdvancedDisplay(title: Chat, description: Chat, icon: Slot, @enumType[VarInt] frame: FrameType, flags: Int,
                             backgroundTexture: Option[Identifier], xCoord: Float, yCoord: Float) extends Structure

  case class Advancement(parentId: Option[Identifier], displayData: Option[AdvancedDisplay],
                         @precededBy[VarInt] criterias: List[Criteria],
                         @precededBy[VarInt] requirments: List[Requirement]) extends Structure

  case class Criterion(criterionIdentifier: Identifier, dateOfAchieving: Option[Long]) extends Structure

  case class Progress(@precededBy[VarInt] criterions: List[Criterion]) extends Structure

  @packet(0x51)
  case class Advancements(resetOrClear: Boolean,
                          @precededBy[VarInt] advancements: List[Advancement],
                          @precededBy[VarInt] identifiers: List[Identifier],
                          @precededBy[VarInt] progresses: List[Progress]) extends Structure

  sealed trait Property //TODO: Model property
  @packet(0x52)
  case class EntityProperties(@boxed entityId: Int,
                              @precededBy[Int] properties: List[Property]) extends Structure

  @packet(0x53)
  case class EntityEffect(@boxed entityId: Int,
                          @byte effectId: Int,
                          @byte amplifier: Int,
                          @boxed duration: Int,
                          @byte flags: Int) extends Structure


  case class Ingredient(@precededBy[VarInt] items: List[Slot]) extends Structure

  sealed trait RecipeType

  @switchKey("crafting_shapeless") case class CraftingShapless(group: String,
                                                                      @precededBy[VarInt] ingredients: List[Ingredient],
                                                                      result: Slot) extends RecipeType

  //TODO: 	Length of ingredient is width * height. Indexed by x + (y * width)
  @switchKey("crafting_shaped") case class CraftingShaped(@boxed width: Int,
                                                                  @boxed height: Int,
                                                                  group: String,
                                                                  ingredients: List[Ingredient],
                                                                  result: Slot) extends RecipeType

  @switchKey("crafting_special_armordye") case class CraftingSpecialArmordye() extends RecipeType

  @switchKey("crafting_special_bookcloning") case class CraftingSpecialBookcloning() extends RecipeType

  @switchKey("crafting_special_mapcloning") case class CraftingSpecialMapcloning() extends RecipeType

  @switchKey("crafting_special_mapextending") case class CraftingSpecialMapextending() extends RecipeType

  @switchKey("crafting_special_firework_rocket") case class CraftingSpecialFireworkRocket() extends RecipeType

  @switchKey("crafting_special_firework_star") case class CraftingSpecialFireworkStar() extends RecipeType

  @switchKey("crafting_special_firework_star_fade") case class CraftingSpecialFireworkStarFade() extends RecipeType

  @switchKey("crafting_special_repairitem") case class CraftingSpecialRepairitem() extends RecipeType

  @switchKey("crafting_special_tippedarrow") case class CraftingSpecialTippedarrow() extends RecipeType

  @switchKey("crafting_special_bannerduplicate") case class CraftingSpecialBannerduplicate() extends RecipeType

  @switchKey("crafting_special_banneraddpattern") case class CraftingSpecialBanneraddpattern() extends RecipeType

  @switchKey("crafting_special_shielddecoration") case class CraftingSpecialShielddecoration() extends RecipeType

  @switchKey("crafting_special_shulkerboxcoloring") case class CraftingSpecialShulkerboxcoloring() extends RecipeType

  @switchKey("smelting") case class Smelting(group: String,
                                             ingredient: Ingredient,
                                             result: Slot,
                                             experience: Float,
                                             @boxed cookingTime: Int) extends RecipeType

  //TODO: check
  case class Recipe(recipeId: Identifier,
                    tpe: String,
                    @fromContext(1) @switchType[String] data: RecipeType) extends Structure

  @packet(0x54)
  case class DeclareRecipes(@precededBy[VarInt] recipes: List[Recipe]) extends Structure

  case class Tag(tagName: Identifier,
                 @precededBy[VarInt] entries: List[VarInt]) extends Structure

  @packet(0x55)
  case class Tags(@precededBy[VarInt] blockTags: List[Tag],
                  @precededBy[VarInt] itemsTags: List[Tag],
                  @precededBy[VarInt] fluidTags: List[Tag]) extends Structure

}
