package io.scalacraft.packets.serverbound

import java.util.UUID

import io.scalacraft.core.marshalling.Structure
import io.scalacraft.core.marshalling.annotations.PacketAnnotations._
import io.scalacraft.packets.DataTypes.{Identifier, Position, Slot, VarInt}

object PlayPackets {

  @packet(0x00)
  case class TeleportConfirm(@boxed teleportId: Int) extends Structure

  @packet(0x01)
  case class QueryBlockNBT(@boxed transactionId: Int, location: Position) extends Structure

  @packet(0x02)
  case class ChatMessage(@maxLength(256) message: String) extends Structure

  sealed trait ClientStatusAction

  object ClientStatusAction {

    @enumValue(0) case object PerformRespawn extends ClientStatusAction

    @enumValue(1) case object RequestStats extends ClientStatusAction

  }

  @packet(0x03)
  case class ClientStatus(@enumType[VarInt] action: ClientStatusAction) extends Structure

  sealed trait ChatMode

  object ChatMode {

    @enumValue(0) case object Enabled extends ChatMode

    @enumValue(1) case object CommandOnly extends ChatMode

    @enumValue(2) case object Hidden extends ChatMode

  }

  sealed trait MainHand

  object MainHand {

    @enumValue(0) case object Left extends MainHand

    @enumValue(1) case object Right extends MainHand

  }

  @packet(0x04)
  case class ClientSettings(@maxLength(16) locale: String, @byte viewDistance: Int,
                            @enumType[VarInt] chatMode: ChatMode, chatColors: Boolean, @byte displayedSkinParts: Int,
                            @enumType[VarInt] mainHand: MainHand) extends Structure

  @packet(0x05)
  case class TabComplete(@boxed transactionId: Int, @maxLength(32500) text: String) extends Structure

  @packet(0x06)
  case class ConfirmTransaction(@byte windowId: Int, @short actionNumber: Int, accepted: Boolean) extends Structure

  @packet(0x07)
  case class EnchantItem(@byte enchantItem: Int, @byte windowId: Int) extends Structure

  @packet(0x08)
  case class ClickWindow(@byte windowId: Int, @short slot: Int, @byte button: Int, @short actionNumber: Int,
                         @boxed mode: Int, clickedItem: Slot) extends Structure

  @packet(0x09)
  case class CloseWindow(@byte windowId: Int) extends Structure

  @packet(0x0A)
  case class PluginMessage(channel: Identifier, data: Array[Byte]) extends Structure
  sealed trait Hand

  object Hand {

    @enumValue(0) case object MainHand extends Hand

    @enumValue(1) case object OffHand extends Hand

  }

  @packet(0x0B)
  case class EditBook(newBook: Slot, isSigning: Boolean, @enumType[VarInt] hand: Hand) extends Structure

  @packet(0x0C)
  case class QueryEntityNBT(@boxed transactionId: Int, @boxed entityId: Int) extends Structure

  sealed trait Tpe

  @switchKey(0) case class Interact(@enumType[VarInt] hand: Hand) extends Tpe

  @switchKey(1) case class Attack() extends Tpe

  @switchKey(2) case class InteractAt(targetX: Float, targetY: Float, targetZ: Float, @enumType[VarInt] hand: Hand) extends Tpe

  @packet(0x0D)
  case class UseEntity(@boxed target: Int, @switchType[VarInt] tpe: Tpe) extends Structure

  @packet(0x0E)
  case class KeepAlive(keepAliveId: Long) extends Structure

  @packet(0x0F)
  case class Player(onGround: Boolean) extends Structure

  @packet(0x10)
  case class PlayerPosition(x: Double, feetY: Double, z: Double, onGround: Boolean) extends Structure

  @packet(0x11)
  case class PlayerPositionAndLook(x: Double, feetY: Double, z: Double, yaw: Float, pitch: Float, onGround: Boolean)
    extends Structure

  @packet(0x12)
  case class PlayerLook(yaw: Float, pitch: Float, onGround: Boolean) extends Structure

  @packet(0x13)
  case class VehicleMove(x: Double, y: Double, z: Double, yaw: Float, pich: Float) extends Structure

  @packet(0x14)
  case class SteerBoat(leftPaddleTurning: Boolean, rightPaddleTurning: Boolean) extends Structure

  @packet(0x15)
  case class PickItem(@boxed slotToUse: Int) extends Structure

  @packet(0x16)
  case class CraftRecipeRequest(@byte windowId: Int, recipe: Identifier, makeAll: Boolean) extends Structure

  @packet(0x17)
  case class PlayerAbilities(@byte flags: Int, flyingSpeed: Float, walkingSpeed: Float) extends Structure

  sealed trait Status

  object Status {

    @enumValue(0) case object StartedDigging extends Status

    @enumValue(1) case object CancelledDigging extends Status

    @enumValue(2) case object FinishedDigging extends Status

    @enumValue(3) case object DropItemStack extends Status

    @enumValue(4) case object DropItem extends Status

    @enumValue(5) case object ShootArrowOrFinishEating extends Status

    @enumValue(6) case object SwapItemInHand extends Status

  }

  sealed trait Face

  object Face {
    @enumValue(0) case object Bottom  extends Face
    @enumValue(1) case object Top extends Face
    @enumValue(2) case object North extends Face
    @enumValue(3) case object South extends Face
    @enumValue(4) case object West extends Face
    @enumValue(5) case object East extends Face
  }

  @packet(0x18)
  case class PlayerDigging(@enumType[VarInt] status: Status, location: Position, @enumType[Byte] face: Face) extends Structure

  sealed trait ActionID

  object ActionID {

    @enumValue(0) case object StartSneaking extends ActionID

    @enumValue(1) case object StopSneaking extends ActionID

    @enumValue(2) case object LeaveBed extends ActionID

    @enumValue(3) case object StartSprinting extends ActionID

    @enumValue(4) case object StopSprinting extends ActionID

    @enumValue(5) case object StartJumpWithHorse extends ActionID

    @enumValue(6) case object StopJumpWithHorse extends ActionID

    @enumValue(7) case object OpenHorseInventory extends ActionID

    @enumValue(8) case object StartFlyingWithElytra extends ActionID

  }

  @packet(0x19)
  case class EntityAction(@boxed entityId: Int, @enumType[VarInt] actionId: ActionID, @boxed jumpBoost: Int) extends Structure

  @packet(0x1A)
  case class SteerVehicle(sideways: Float, forward: Float, @byte flags: Int) extends Structure

  sealed trait SwitchInterface

  @switchKey(0) case class DisplayedRecipe(recipeId: Identifier) extends SwitchInterface

  @switchKey(1) case class RecipeBookStates(craftingRecipeBookOpen: Boolean,
                                            craftingRecipeFilterActive: Boolean,
                                            smeltingRecipeBookOpen: Boolean,
                                            smeltingRecipeFilterActive: Boolean) extends SwitchInterface

  @packet(0x1B)
  case class RecipeBookData(@switchType[VarInt] restOfPacket: SwitchInterface) extends Structure

  @packet(0x1C)
  case class NameItem(itemName: String) extends Structure

  sealed trait Result

  object Result {

    @enumValue(0) case object SuccessfullyLoaded extends Result

    @enumValue(1) case object Declined extends Result

    @enumValue(2) case object FailedDownload extends Result

    @enumValue(3) case object Accepted extends Result

  }

  @packet(0x1D)
  case class ResourcePacketStatus(@enumType[VarInt] result: Result) extends Structure

  sealed trait ActionPacket1E

  object ActionPacket1E {

    @enumValue(0) case object OpenedTab extends ActionPacket1E

    @enumValue(1) case object ClosedScreen extends ActionPacket1E

  }

  @packet(0x1E)
  case class AdvancementTab(@enumType[VarInt] action: ActionPacket1E, tabId: Option[Identifier]) extends Structure

  @packet(0x1F)
  case class SelectTrade(@boxed selectedSlot: Int) extends Structure

  @packet(0x20)
  case class SetBeaconEffect(@boxed primaryEffect: Int, @boxed secondaryEffect: Int) extends Structure

  @packet(0x21)
  case class HeldItemChange(@short slot: Int) extends Structure

  sealed trait ModePacket22

  object ModePacket22 {

    @enumValue(0) case object Sequence extends ModePacket22

    @enumValue(1) case object Auto extends ModePacket22

    @enumValue(2) case object Redstone extends ModePacket22

  }

  @packet(0x22)
  case class UpdateCommandBlock(location: Position, command: String, @enumType[VarInt] mode: ModePacket22,
                                @byte flags: Int) extends Structure

  @packet(0x23)
  case class UpdateCommandBlockMinecart(@boxed entityId: Int, command: String, trackOutput: Boolean) extends Structure

  @packet(0x24)
  case class CreativeInventoryAction(@short slot: Int, clickedItem: Slot) extends Structure

  sealed trait ActionPacket25

  case object ActionPacket25 {

    @enumValue(0) case object UpdateData extends ActionPacket25

    @enumValue(1) case object SaveTheStructure extends ActionPacket25

    @enumValue(2) case object LoadTheStructure extends ActionPacket25

    @enumValue(3) case object DetectSize extends ActionPacket25

  }

  sealed trait ModePacket25

  case object ModePacket25 {

    @enumValue(0) case object Save extends ModePacket25

    @enumValue(1) case object Load extends ModePacket25

    @enumValue(2) case object Corner extends ModePacket25

    @enumValue(3) case object Data extends ModePacket25

  }

  sealed trait Mirror

  case object Mirror {

    @enumValue(0) case object None extends Mirror

    @enumValue(1) case object LeftRight extends Mirror

    @enumValue(2) case object FrontBack extends Mirror

  }

  sealed trait RotationType

  case object RotationType {

    @enumValue(0) case object None extends RotationType

    @enumValue(1) case object ClockWise90 extends RotationType

    @enumValue(2) case object ClockWise180 extends RotationType

    @enumValue(3) case object CounterClockWise90 extends RotationType

  }

  @packet(0x25)
  case class UpdateStructureBlock(location: Position, @enumType[VarInt] action: ActionPacket25,
                                  @enumType[VarInt] mode: ModePacket25, name: String, @byte offsetX: Int,
                                  @byte offsetY: Int, @byte offesetZ: Int, @byte sizeX: Int, @byte sizeY: Int,
                                  @byte sizeZ: Int, @enumType[VarInt] mirror: Mirror,
                                  @enumType[VarInt] rotation: RotationType, metadata: String, integrity: Float,
                                  @boxed seed: Long, @byte flags: Int) extends Structure

  @packet(0x26)
  case class UpdateSign(location: Position, @maxLength(384) line1: String, @maxLength(384) line2: String,
                        @maxLength(384) line3: String, @maxLength(384) line4: String) extends Structure

  @packet(0x27)
  case class Animation(@enumType[VarInt] hand: Hand) extends Structure

  @packet(0x28)
  case class Spectate(targetPlayer: UUID) extends Structure

  @packet(0x29)
  case class PlayerBlockPlacement(location: Position, @enumType[VarInt] face: Face, @enumType[VarInt] hand: Hand,
                                  cursorPositionX: Float, cursorPositionY: Float,
                                  cursorPosiontZ: Float) extends Structure

  @packet(0x2A)
  case class UseItem(@enumType[VarInt] hand: Hand) extends Structure
}
