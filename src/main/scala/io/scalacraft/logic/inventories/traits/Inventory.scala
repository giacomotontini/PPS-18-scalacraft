package io.scalacraft.logic.inventories.traits

import io.scalacraft.loaders.Items
import io.scalacraft.logic.inventories.InventoryItem
import io.scalacraft.logic.inventories.traits.range.MainHotRange

trait Inventory {

  protected val inventory: Array[Option[InventoryItem]]
  protected[inventories] val mainHotInventoryRange: MainHotRange

  def id: Int

  private def fillSlot(slot: Int, quantity: Int, slotCapacity: Int, itemId: Int): Int = {
    val stockedQuantity = inventory(slot) match {
      case Some(item) => item.quantity
      case None => 0
    }

    if (stockedQuantity < slotCapacity) {
      if (quantity <= slotCapacity - stockedQuantity) {
        inventory(slot) match {
          case Some(item) => item.quantity += quantity
          case None => inventory(slot) = Some(InventoryItem(itemId, quantity))
        }
        0
      } else {
        inventory(slot) match {
          case Some(item) => item.quantity = slotCapacity
          case None => inventory(slot) = Some(InventoryItem(itemId, slotCapacity))
        }
        quantity - (slotCapacity - stockedQuantity)
      }
    } else
      quantity
  }

  def addItem(slot: Int, inventoryItem: InventoryItem): Int = {
    val stackSize = Items.getStorableItemById(inventoryItem.itemId).stackSize
    fillSlot(slot, inventoryItem.quantity, stackSize, inventoryItem.itemId)
  }

  def addItem(inventoryItem: InventoryItem): Int = {

    def fillSlots(slots: List[Int], stackSize: Int, quantity: Int): Int = {
      var leftQuantity = quantity
      for (slot <- slots if leftQuantity > 0) {
        leftQuantity = fillSlot(slot, leftQuantity, stackSize, inventoryItem.itemId)
      }
      leftQuantity
    }

    val stackSize = Items.getStorableItemById(inventoryItem.itemId).stackSize
    val quantity = fillSlots(findItemsIndex(inventoryItem.itemId), stackSize, inventoryItem.quantity)
    fillSlots(findAvailableIndex, stackSize, quantity)
  }

  def removeItem(slot: Int, inventoryItem: InventoryItem): Unit = removeItem(slot, inventoryItem.quantity)

  def removeItem(slot: Int, quantity: Int): Unit = inventory(slot) match {
    case Some(item) =>
      inventory(slot).get.quantity -= quantity
      if (item.quantity <= 0)
        clearSlot(slot)
    case None =>
  }

  def clearSlot(slot: Int): Unit = inventory(slot) = None

  def moveItem(from: Int, to: Int, quantity: Int): Unit = inventory(from) match {
    case Some(fromItem) =>
      val quantityToBeMoved = math.min(fromItem.quantity, quantity)
      val exceedQuantity = addItem(to, InventoryItem(fromItem.itemId, quantityToBeMoved))
      fromItem.quantity -= quantityToBeMoved - exceedQuantity
      if (fromItem.quantity == 0) {
        inventory(from) = None
      }
    case None =>
  }

  //Retrieve all items, included non inventory one (like crafting output, armors etc.)
  def retrieveAllItems: List[Option[InventoryItem]] = inventory.map(identity).toList

  def retrieveInventoryItems: List[Option[InventoryItem]] =
    inventory.slice(mainHotInventoryRange.MainInventorySlotRange.start, mainHotInventoryRange.HotBarSlotRange.end + 1)
      .map(identity).toList

  def findAvailableIndex: List[Int]

  def findItemsIndex(itemId: Int): List[Int]

  def inventoryClosed(): Any

}
