package io.scalacraft.logic

import scala.collection.mutable

case class InventoryItem(itemId: Int, var quantity: Int = 0)

sealed trait Inventory {
  private val inventory = mutable.MutableList[Option[InventoryItem]]()

  def addItem(slot: Int, inventoryItem: InventoryItem): Unit = {
    if(inventory(slot).isDefined && inventory(slot).get.itemId == inventoryItem.itemId)
      inventory(slot).get.quantity += inventoryItem.quantity
    else {
      inventory(slot) = Some(inventoryItem)
    }
  }

  def addItem(inventoryItem: InventoryItem): Unit = addItem(findAvailableIndex(), inventoryItem)

  def removeItem(slot: Int, inventoryItem: InventoryItem): Unit = {
    if(inventory(slot).isDefined && inventory(slot).get.itemId == inventoryItem.itemId) {
      inventory(slot).get.quantity -= inventoryItem.quantity
      if (inventory(slot).get.quantity <= 0)
        inventory(slot) = None
    }
  }

  def moveItem(from: Int, to: Int, quantity: Int): Unit = {
    if(inventory(from).isDefined && inventory(from).get.quantity >= quantity) {
      inventory(from).get.quantity -= quantity
      if(inventory(to).isDefined) {
        inventory(to).get.quantity += quantity
      } else {
        inventory(to) = Some(InventoryItem(inventory(from).get.itemId, quantity))
      }
    }
  }

  def retrieveAllItems(): List[Option[InventoryItem]] = {
    inventory.toList
  }

  private def findAvailableIndex(): Int ={
    inventory.indexWhere(_== None)
  }
}


sealed trait CraftingInventoty
sealed trait ChestInventory

case class PlayerInventory() extends Inventory with CraftingInventoty {
  //All range boundaries are inclusive by protocol
  private val CrafitingOutputSlot = 0
  private val CraftingInputSlotRange = Range(1,4)
  private val ArmorSlotRange = Range(5, 8)
  private val MainInventorySlotRange = Range(9, 35)
  private val HotBarSlotRange = Range(36, 44)
  private val OffhandSlot = 45
}

case class Chest() extends Inventory with ChestInventory {
  private val ChestSlotRange = Range(0, 26)
  private val MainInventorySlotRange = Range(27, 53)
  private val HotBarSlotRange = Range(54, 62)
}

case class LargeChest() extends Inventory with ChestInventory {
  private val ChestSlotRange = Range(0, 53)
  private val MainInventorySlotRange = Range(53, 80)
  private val HotBarSlotRange = Range(81, 89)
}

case class CraftingTable() extends Inventory with CraftingInventoty {
  private val CrafitingOutputSlot = 0
  private val CraftingInputSlotRange = Range(1,9)
  private val MainInventorySlotRange = Range(10, 36)
  private val HotBarSlotRange = Range(37, 45)

}

