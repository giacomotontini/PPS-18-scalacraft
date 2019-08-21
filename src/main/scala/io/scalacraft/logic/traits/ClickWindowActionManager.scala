package io.scalacraft.logic.traits

import akka.actor.{Actor, ActorRef}
import io.scalacraft.logic.messages.Message.InventoryDropItems
import io.scalacraft.logic.{Inventory, InventoryItem, InventoryWithCrafting}
import io.scalacraft.packets.DataTypes.{Slot, SlotData}
import io.scalacraft.packets.serverbound.PlayPackets.ClickWindowAction
import io.scalacraft.packets.serverbound.PlayPackets.ClickWindowAction.{LeftMouseClick, LeftMouseDrag, RightMouseClick, RightMouseDrag}
import net.querz.nbt.CompoundTag
import org.slf4j.{Logger, LoggerFactory}

trait ClickWindowActionManager {

  protected val player: ActorRef
  private def OutSlot: Int = -999
  private def DummySlot: Int = -1
  private def log: Logger = LoggerFactory.getLogger(getClass)

  protected val inventory: Inventory
  private var holdItems: Slot = None //items that are being moved throw mouse cursor


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

  private def swapHoldItems(slot: Int, holdingItems: SlotData, slotItems: SlotData): Slot = {
    log.debug("Swap hold items")
    inventory.removeItem(slot, InventoryItem(slotItems.itemId, slotItems.itemCount))
    inventory.addItem(slot, InventoryItem(holdingItems.itemId, holdingItems.itemCount))
    Some(slotItems)
  }

  private def holdNewItems(slot: Int, slotItems: SlotData): Slot = {
    log.debug("Hold new items")
    inventory.removeItem(slot, InventoryItem(slotItems.itemId, slotItems.itemCount))
    Some(slotItems)
  }

  private def releaseHoldItems(quantity: Int, holdingItems: SlotData) : Slot = (holdingItems.itemCount - 1) match {
      case left if left > 0 => Some(SlotData(holdingItems.itemId, left, new CompoundTag()))
      case _ => None
  }

  private def releaseHoldItemsOnSlot(slot: Int, holdingItems: SlotData, oneByOne: Boolean = false): Slot = {
    if (oneByOne) {
      log.debug("Release new items one by one.")
      inventory.addItem(slot, InventoryItem(holdingItems.itemId, 1))
      releaseHoldItems(1, holdingItems)
    } else {
      log.debug("Release new items.")
      inventory.addItem(slot, InventoryItem(holdingItems.itemId, holdingItems.itemCount))
      None
    }
  }

  private def splitSlotItems(slot: Int, slotItems: SlotData): Slot = {
    log.debug("Splitting items.")
    val movingQuantityLeft: Int = (slotItems.itemCount + 1) / 2 // the highest half of items are kept on hand
    inventory.removeItem(slot, InventoryItem(slotItems.itemId, movingQuantityLeft))
    Some(SlotData(slotItems.itemId, movingQuantityLeft, new CompoundTag()))
  }

  def handleAction(action: ClickWindowAction, slot: Int, slotItems: Slot, craftingSlot: Boolean = false): Unit = {
    def _handleAction(): Unit = {
      action match {
        case LeftMouseClick(_) if slot == OutSlot && holdItems.isDefined =>
          player ! InventoryDropItems(holdItems.get.itemId, holdItems.get.itemCount)
          holdItems = None
        case LeftMouseClick(false) | LeftMouseDrag(true, false) if slot != DummySlot=>
          slotItems match {
            case Some(slotItems) =>
              holdItems match {
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
        case RightMouseClick(_) if slot == OutSlot && holdItems.isDefined =>
          player ! InventoryDropItems(holdItems.get.itemId, 1)
          holdItems = releaseHoldItems(1, holdItems.get)
        case RightMouseClick(false) | RightMouseDrag(true, false) if slot != DummySlot =>
          slotItems match {
            case Some(slotItems) =>
              holdItems match {
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
    }

    def _handleActionOnCraftingSlot(): Unit = {
      action match {
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
    }


    if (craftingSlot) {
      _handleActionOnCraftingSlot()
    } else {
      _handleAction()
    }
  }
}
