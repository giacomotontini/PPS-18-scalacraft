package io.scalacraft.logic.inventories.traits

import akka.actor.ActorRef
import io.scalacraft.logic.commons.Message.InventoryDropItems
import io.scalacraft.logic.inventories.InventoryItem
import io.scalacraft.packets.DataTypes.{Slot, SlotData}
import io.scalacraft.packets.serverbound.PlayPackets.ClickWindowAction
import io.scalacraft.packets.serverbound.PlayPackets.ClickWindowAction.{LeftMouseClick, LeftMouseDrag, RightMouseClick, RightMouseDrag}
import net.querz.nbt.CompoundTag
import org.slf4j.{Logger, LoggerFactory}

/**
 * A manager for each click type operation made by the user on an inventory.
 */
trait ClickWindowActionManager {

  protected val player: ActorRef // the player which did the "click".

  private val OutSlot: Int = -999 // the slot number representing the area outside of the inventory window.

  /*
   * The slot number representing operation that should not be taken into account.
   * Used to mark an operation as illegal; is sent by the client, sent to tell the server an illegal operation is made.
   */
  private val DummySlot: Int = -1

  private val log: Logger = LoggerFactory.getLogger(getClass)

  protected val inventory: Inventory
  private var holdItems: Slot = None //items that are being moved throw mouse cursor

  /**
   * Group items of the same type on a specific slot.
   * @param slot the slot on which the user clicked
   * @param holdingItems the items actually held by mouse's cursor
   * @return optionally, the items that couldn't be grouped and must be kept on cursor
   */
  private def groupItems(slot: Int, holdingItems: SlotData): Option[SlotData] =
    inventory.addItem(slot, InventoryItem(holdingItems.itemId, holdingItems.itemCount)) match {
      case 0 =>
        log.debug("Grouping items withOUT items left.")
        None
      case left =>
        log.debug("Grouping items with items left.")
        Some(SlotData(holdingItems.itemId, left, new CompoundTag()))
    }

  /**
   * Swap the items held on cursor with those on a specific slot. Used when the two item group are of a different type.
   * @param slot the slot on which the user clicked
   * @param holdingItems the items actually held by mouse's cursor
   * @param slotItems the items kept in the clicked slot
   * @return the new items that must be kept on cursor
   */
  private def swapHoldItems(slot: Int, holdingItems: SlotData, slotItems: SlotData): Slot = {
    log.debug("Swap hold items")
    inventory.removeItem(slot, InventoryItem(slotItems.itemId, slotItems.itemCount))
    inventory.addItem(slot, InventoryItem(holdingItems.itemId, holdingItems.itemCount))
    Some(slotItems)
  }

  /**
   * Hold items from a specific slot, leaving it empty.
   * @param slot the slot on which the user clicked
   * @param slotItems the items kept in the clicked slot
   * @return the new items that must be kept on cursor
   */
  private def holdNewItems(slot: Int, slotItems: SlotData): Slot = {
    log.debug("Hold new items")
    inventory.removeItem(slot, InventoryItem(slotItems.itemId, slotItems.itemCount))
    Some(slotItems)
  }

  /**
   * Release a fixed quantity of items from cursor. It doesn't affect the inventory, it just throw away held item.
   * @param quantity the quantity to be released on a specific slot
   * @param holdingItems the items actually held by mouse's cursor
   * @return the new items that must be kept on cursor
   */
  private def releaseHoldItems(quantity: Int, holdingItems: SlotData): Slot = holdingItems.itemCount - 1 match {
    case left if left > 0 => Some(SlotData(holdingItems.itemId, left, new CompoundTag()))
    case _ => None
  }

  /**
   * Release a fixed quantity of items from cursor on a specific slot.
   * @param slot the slot on which the user clicked
   * @param holdingItems the items actually held by mouse's cursor
   * @param oneByOne release one item at a time
   * @return the new items that must be kept on cursor
   */
  private def releaseHoldItemsOnSlot(slot: Int, holdingItems: SlotData, oneByOne: Boolean = false): Slot =
    if (oneByOne) {
      log.debug("Release new items one by one.")
      inventory.addItem(slot, InventoryItem(holdingItems.itemId, 1))
      releaseHoldItems(1, holdingItems)
    } else {
      log.debug("Release new items.")
      inventory.addItem(slot, InventoryItem(holdingItems.itemId, holdingItems.itemCount))
      None
    }

  /**
   * Split the items held on a specific slot. The lowest half are left on slot, the other it's taken on cursor
   * @param slot  the slot on which the user clicked
   * @param slotItems the items kept in the clicked slot
   * @return the new items that must be kept on cursor
   */
  private def splitSlotItems(slot: Int, slotItems: SlotData): Slot = {
    log.debug("Splitting items.")
    val movingQuantityLeft: Int = (slotItems.itemCount + 1) / 2 // the highest half of items are kept on cursor
    inventory.removeItem(slot, InventoryItem(slotItems.itemId, movingQuantityLeft))
    Some(SlotData(slotItems.itemId, movingQuantityLeft, new CompoundTag()))
  }

  /**
   * Handle an action performed on a slot, taking into account the held items.
   * @param action the action that must be handled
   * @param slot the slot on which the action is related
   * @param slotItems the items kept within the clicked slot
   * @param isCraftingOutputSlot is the affected slot a crafting output slot? Only the calling inventory knows that.
   */
  def handleAction(action: ClickWindowAction, slot: Int, slotItems: Slot, isCraftingOutputSlot: Boolean = false): Unit = {

    /*
     * Handle action on traditional slot
     */
    def _handleAction(): Unit = action match {
      /* Action started or concluded with a left click */
      case LeftMouseClick(_) if slot == OutSlot && holdItems.isDefined =>
        player ! InventoryDropItems(holdItems.get.itemId, holdItems.get.itemCount)
        holdItems = None
      case LeftMouseClick(false) | LeftMouseDrag(true, false) if slot != DummySlot => slotItems match {
        case Some(slotItems) => holdItems match {
          case Some(holdItems) if holdItems.itemId == slotItems.itemId =>
            this.holdItems = groupItems(slot, holdItems)
          case Some(holdItems) =>
            this.holdItems = swapHoldItems(slot, holdItems, slotItems)
          case None =>
            holdItems = holdNewItems(slot, slotItems)
        }
        case None if holdItems.isDefined =>
          holdItems = releaseHoldItemsOnSlot(slot, holdItems.get)
        case None =>
      }
      /* Action started or concluded with a right click */
      case RightMouseClick(_) if slot == OutSlot && holdItems.isDefined =>
        player ! InventoryDropItems(holdItems.get.itemId, 1)
        holdItems = releaseHoldItems(1, holdItems.get)
      case RightMouseClick(false) | RightMouseDrag(true, false) if slot != DummySlot => slotItems match {
        case Some(slotItems) => holdItems match {
          case Some(holdItems) if holdItems.itemId == slotItems.itemId =>
            this.holdItems = releaseHoldItemsOnSlot(slot, holdItems, oneByOne = true)
          case Some(holdItems) =>
            this.holdItems = swapHoldItems(slot, holdItems, slotItems)
          case None =>
            holdItems = splitSlotItems(slot, slotItems)
        }
        case None if holdItems.isDefined =>
          holdItems = releaseHoldItemsOnSlot(slot, holdItems.get, oneByOne = true)
        case None => //ignored
      }
      case _ => //ignored
    }

    /*
     * Handle action on crafting slot. All the action in that case have different consequences.
     */
    def _handleActionOnCraftingSlot(): Unit = action match {
      case LeftMouseClick(_) | LeftMouseDrag(true, false) | RightMouseClick(_) | RightMouseDrag(true, false) =>
        slotItems match {
          case Some(slotItems) =>
            holdItems match {
              case Some(holdItems) if holdItems.itemId == slotItems.itemId =>
                this.holdItems = Some(SlotData(holdItems.itemId, holdItems.itemCount + slotItems.itemCount, new CompoundTag))
              case None =>
                holdItems = holdNewItems(slot, slotItems)
            }
            inventory.asInstanceOf[InventoryWithCrafting].craftingAccepted()
          case None => //ignored
        }
      case _ => //ignored
    }

    if (isCraftingOutputSlot) _handleActionOnCraftingSlot() else _handleAction()
  }
}
