package io.scalacraft.core.clientbound

import java.util.Locale.Category
import java.util.UUID

import io.scalacraft.core.DataTypes.{Angle, Chat, Direction, Nbt, Position, Slot, VarInt}
import io.scalacraft.core.Entities.{Entity, MobEntity, Player}
import io.scalacraft.core.PacketAnnotations._
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
                         @short velocityZ: Int)

  @packet(id = 0x01)
  case class SpawnExperienceOrb(@boxed entityId: Int,
                                x: Double,
                                y: Double,
                                z: Double,
                                @short count: Int)


  sealed trait GlobalEntityType
  case object GlobalEntityType {
    @enumValue(1) case object Thunderbolt extends GlobalEntityType
  }

  @packet(id = 0x02)
  case class SpawnGlobalEntity(@boxed entityId: Int,
                               @enumType[Byte] tpe: Int,
                               x: Double,
                               y: Double,
                               z: Double)

  @packet(id = 0x03)
  case class SpawnMob(@boxed entityId: Int,
                      entityUUID: UUID,
                      @boxed tpe: Int,
                      x: Double,
                      y: Double,
                      z: Double,
                      yaw: Angle,
                      pitch:  Angle,
                      headPitch: Angle,
                      velocityX: Short,
                      velocityY: Short,
                      velocityZ: Short,
                      @fromContext(2) metadata: MobEntity
                     )

  @packet(id = 0x04)
  case class SpawnPainting(@boxed entityId: Int,
                           entityUUID: UUID,
                           @boxed motive: Int,
                           location: Position, //center coordinate, see wiki for computer image centering
                           @enumType[Byte] direction: Direction)

  @packet(id = 0x05)
  case class SpawnPlayer(@boxed entityId: Int,
                         playerUUID: UUID,
                         x: Double,
                         y: Double,
                         z: Double,
                         yaw: Angle,
                         pitch: Angle,
                         metadata: Player) //TODO: we do not have type, should be handled

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
                       @enumType[Byte] animation: Animation)

  case class CategoryStatistic(@enumType[VarInt] categoryId: CategoriesType,
                               @boxed statisticId: Int) extends  Structure

  sealed trait CategoriesType
  case object CategoriesType{
    @enumValue(value = 0) case object Mined extends CategoriesType
    @enumValue(value = 1) case object Crafted extends CategoriesType
    @enumValue(value = 2) case object Used extends CategoriesType
    @enumValue(value = 3) case object Broken extends CategoriesType
    @enumValue(value = 4) case object PickedUp extends CategoriesType
    @enumValue(value = 5) case object Dropped extends CategoriesType
    @enumValue(value = 6) case object Killed extends CategoriesType
    @enumValue(value = 7) case object KilledBy extends CategoriesType
    @enumValue(value = 8) case object Custumo extends CategoriesType
  }

  @packet(id = 0x07)
  case class Statistics(@boxed count: Int,
                        @precededBy[VarInt] statistic: List[CategoryStatistic],
                        @boxed value: Int)


  @packet(id = 0x08)
  case class BlockBreakAnimation(@boxed entityId: Int,
                                 location: Position,
                                @byte destroyStage: Int) //0-9 normal-bad stage, > 9 destroyed

  @packet(id = 0x09)
  case class UpdateBlockEntity(location: Position, action: Int, nbtData: Nbt) //TODO: see protocol to understabd action data meaning

  @packet(id = 0x0A)
  case class BlockAction(location: Position,
                         @byte actionId: Int,
                         @byte actionParam: Int,
                         @boxed blockType: Int)

  @packet(id = 0x0B)
  case class BlockChange(location: Position,
                         @boxed blockId: Int)

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
  case class ServerDifficulty(@enumType[Byte] @byte difficulty: ServerDifficulties)

  sealed trait ChatPosition
  case object ChatPosition {
    @enumValue(0) case object ChatBox extends ChatPosition
    @enumValue(1) case object SystemMessage extends ChatPosition
    @enumValue(2) case object GameInfo extends ChatPosition
  }

  @packet(id = 0x0E)
  case class ChatMessage(jsonData: Chat, @enumType[Byte] position: ChatPosition)

  case class MultiBlockChangeRecord(@byte horizontalPosition: Int,
                                   @byte yCoordinate: Int,
                                   @boxed blockId: Int) extends Structure

  @packet(id = 0x0F)
  case class MultiBlockChange(chunkX: Int,
                              chunkY: Int,
                              @boxed recordCount: Int,
                              @precededBy[VarInt] record: List[MultiBlockChangeRecord]) //check protocol for position decoding algorithm

  case class TabCompleteMatches(tabMatch: String,
                                tooltip: Option[Chat]) extends Structure

  @packet(id = 0x10)
  case class TabComplete(@boxed id: Int,
                         @boxed start: Int,
                         @boxed length: Int,
                         @boxed count: Int,
                         @precededBy[VarInt] matches: List[TabCompleteMatches])

  /* TODO: support NODE data type
  @packet(id = 0x11)
  case class DeclareCommands(@boxed count: Int,
                             @precededBy[VarInt] nodes: List[Node],
                             @boxed rootIndex: Int)
   */

  @packet(id = 0x12)
  case class ConfirmTransaction(@byte windowsId: Int,
                                @short actionNumber: Int,
                                accepted: Boolean)

  @packet(id = 0x13)
  case class CloseWindows(@byte windowId: Int)

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
                         @fromContext(2) entityId: Option[Int]) //TODO: the option presence is determined by a string value => how we handle that?

  @packet(0x15)
  case class WindowItems(@byte windowId: Int, @short count:Int, @precededBy[Short] slot: List[Slot] ) extends Structure

  

}
