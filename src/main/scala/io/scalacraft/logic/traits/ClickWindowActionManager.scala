package io.scalacraft.logic.traits




import io.scalacraft.logic.{Inventory, InventoryItem}
import io.scalacraft.packets.DataTypes.{Slot, SlotData}
import io.scalacraft.packets.serverbound.PlayPackets.ClickWindowAction
import io.scalacraft.packets.serverbound.PlayPackets.ClickWindowAction.{LeftMouseClick, LeftMouseDrag, RightMouseClick, RightMouseDrag}
import net.querz.nbt.CompoundTag
import org.slf4j.{Logger, LoggerFactory}

trait ClickWindowActionManager{

  private def log : Logger = LoggerFactory.getLogger(getClass)

  protected val inventory: Inventory
  private var holdedItems: Slot = None  //items that are being moved throw mouse cursor


  private def groupItems(slot: Int, holdingItems: SlotData): Option[SlotData] = {
    inventory.addItem(slot, InventoryItem(holdingItems.itemId, holdingItems.itemCount)) match {
      case 0 =>
        log.debug("Grouping items withOUT items left.")
        None
      case left =>
        log.debug("Grouping items with items left.")
        Some(SlotData(holdingItems.itemId, left, new CompoundTag()))
    }
  }

  private def swapHoldedItems(slot: Int, holdingItems: SlotData, slotItems: SlotData): Slot = {
    log.debug("Swap holded items")
    inventory.removeItem(slot, InventoryItem(slotItems.itemId, slotItems.itemCount))
    inventory.addItem(slot, InventoryItem(holdingItems.itemId, holdingItems.itemCount))
    Some(slotItems)
  }

  private def holdNewItems(slot: Int, slotItems: SlotData): Slot = {
    log.debug("Hold new items")
    inventory.removeItem(slot, InventoryItem(slotItems.itemId, slotItems.itemCount))
    Some(slotItems)
  }

  private def releaseNewItems(slot: Int, holdingItems: SlotData, oneByOne: Boolean = false): Slot = {
    if(oneByOne) {
        log.debug("Release new items one by one.")
      inventory.addItem(slot, InventoryItem(holdingItems.itemId, 1))
      (holdingItems.itemCount - 1) match {
         case left if left > 0 => Some(SlotData(holdingItems.itemId, left, new CompoundTag()))
         case _ => None
       }
    } else {
      log.debug("Release new items.")
      inventory.addItem(slot, InventoryItem(holdingItems.itemId, holdingItems.itemCount))
      None
    }
  }

  def splitSlotItems(slot: Int, slotItems: SlotData): Slot = {
    log.debug("Splitting items.")
    val movingQuantityLeft: Int = (slotItems.itemCount + 1) / 2 // the highest half of items are kept on hand
    inventory.removeItem(slot, InventoryItem(slotItems.itemId, movingQuantityLeft))
    Some(SlotData(slotItems.itemId, movingQuantityLeft, new CompoundTag()))
  }

  def handleAction(action: ClickWindowAction, slot: Int, slotItems: Slot): Unit = {
    action match {
      case LeftMouseClick(_) | LeftMouseDrag(true, false) =>
        slotItems match {
          case Some(slotItems) =>
            holdedItems match {
              case Some(holdedItems) if holdedItems.itemId == slotItems.itemId =>
                this.holdedItems = groupItems(slot, holdedItems)
              case Some(holdedItems) =>
                this.holdedItems = swapHoldedItems(slot, holdedItems, slotItems)
              case None =>
                holdedItems = holdNewItems(slot, slotItems)
            }
          case None if holdedItems.isDefined =>
            holdedItems = releaseNewItems(slot, holdedItems.get)
          case None =>
        }

      case RightMouseClick(_) | RightMouseDrag(true, false) =>
        slotItems match {
          case Some(slotItems) =>
            holdedItems match {
              case Some(holdedItems) if holdedItems.itemId == slotItems.itemId =>
                this.holdedItems = releaseNewItems(slot, holdedItems, oneByOne = true)
              case Some(holdedItems) =>
                this.holdedItems = swapHoldedItems(slot, holdedItems, slotItems)
              case None =>
                holdedItems = splitSlotItems(slot, slotItems)
          }
          case None if holdedItems.isDefined =>
            holdedItems = releaseNewItems(slot, holdedItems.get , oneByOne = true)
          case None =>
        }
      case _ => //ignored
    }

  }

}
