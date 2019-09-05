package io.scalacraft.logic.inventories.traits

import io.scalacraft.loaders.Items
import io.scalacraft.logic.inventories.InventoryItem
import io.scalacraft.logic.inventories.traits.range.MainHotRange

/**
 * A base inventory.
 */
trait Inventory {

  protected val inventory: Array[Option[InventoryItem]]
  protected[inventories] val mainHotInventoryRange: MainHotRange

  def id: Int

  /**
   * Tries to fill a slot of a given maximum capacity with a certain quantity of items.
   *
   * @param slot         the slot that must be filled
   * @param quantity     the quantity of item that must be added
   * @param slotCapacity the maximum number of items that the slot can hold
   * @param itemId       the item id of the items
   * @return the quantity left that cannot be inserted into the slot
   */
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

  /**
   * Add an inventory item to a specific slot
   *
   * @param slot          the slot on which the item should be added
   * @param inventoryItem the item that should be added (with quantity)
   * @return the quantity left that cannot be added
   */
  def addItem(slot: Int, inventoryItem: InventoryItem): Int = {
    val stackSize = Items.getItemById(inventoryItem.itemId).stackSize
    fillSlot(slot, inventoryItem.quantity, stackSize, inventoryItem.itemId)
  }

  /**
   * Add an inventory item to all compatible slot until exhaustion.
   *
   * @param inventoryItem the item that should be added (with quantity)
   * @return the quantity left that cannot be added
   */
  def addItem(inventoryItem: InventoryItem): Int = {

    def fillSlots(slots: List[Int], stackSize: Int, quantity: Int): Int = {
      var leftQuantity = quantity
      for (slot <- slots if leftQuantity > 0) {
        leftQuantity = fillSlot(slot, leftQuantity, stackSize, inventoryItem.itemId)
      }
      leftQuantity
    }

    /*
    Tries to add the desired quantity to all slot containing the same item type until maximum stack size.
    If more must be added, free available slots are filled
    */
    val stackSize = Items.getItemById(inventoryItem.itemId).stackSize
    val quantity = fillSlots(findItemsIndex(inventoryItem.itemId), stackSize, inventoryItem.quantity)
    fillSlots(findAvailableIndex, stackSize, quantity)
  }

  /**
   * Remove an inventory item from a specific slot, if present
   * @param slot the slot from which the item should be removed
   * @param inventoryItem the inventory item to be removed (with quantity)
   */
  def removeItem(slot: Int, inventoryItem: InventoryItem): Unit = removeItem(slot, inventoryItem.quantity)

  /**
   * Remove from a specific slot the specified quantity. Type is not considered.
   * @param slot the slot from which items should be removed
   * @param quantity the quantity to be removed
   */
  def removeItem(slot: Int, quantity: Int): Unit = inventory(slot) match {
    case Some(item) =>
      inventory(slot).get.quantity -= quantity
      if (item.quantity <= 0)
        clearSlot(slot)
    case None =>
  }

  /**
   * Empty a slot.
   * @param slot the slot to be cleared
   */
  def clearSlot(slot: Int): Unit = inventory(slot) = None

  /**
   * Move items from one slot to another, according to stack sizes.
   * @param from the source slot
   * @param to the destination slot
   * @param quantity the quantity to be moved
   */
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

  /**
   * Retrieve all items, included non inventory one (like crafting output, armors etc.)
   * @return a list of all the items
   */
  def retrieveAllItems: List[Option[InventoryItem]] = inventory.map(identity).toList

  /**
   * Retrieve the items within player inventory ranges.
   * @return a list of player inventory items
   */
  def retrieveInventoryItems: List[Option[InventoryItem]] =
    inventory.slice(mainHotInventoryRange.MainInventorySlotRange.start, mainHotInventoryRange.HotBarSlotRange.end + 1)
      .map(identity).toList

  /**
   * @return a list of all available indexes on the player inventory area
   */
  def findAvailableIndex: List[Int]

  /**
   * @param itemId the item id of the items
   * @return a list of all indexes on which items id are stored (from the player inventory area)
   */
  def findItemsIndex(itemId: Int): List[Int]

  /**
   * Closes the inventory. Depends on specific implementation.
   */
  def inventoryClosed(): Unit

}
