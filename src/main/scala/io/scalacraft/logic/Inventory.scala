package io.scalacraft.logic

import io.scalacraft.loaders.Items

case class InventoryItem(itemId: Int, var quantity: Int = 0)

sealed trait Inventory {
  protected val inventory: Array[Option[InventoryItem]]

  private def fillSlot(slot: Int, quantity: Int, slotCapacity: Int, itemId: Int): Int = {
    val stockedQuantity = inventory(slot) match {
        case Some(item) => item.quantity
        case None => 0
    }

    if (stockedQuantity < slotCapacity) {
      if (quantity <= slotCapacity - stockedQuantity ) {
        inventory(slot) match {
          case Some(item) => item.quantity += quantity
          case None => inventory(slot) = Some(InventoryItem(itemId, quantity))
        }
        0
      } else {
        inventory(slot) match  {
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

  def addItem(inventoryItem: InventoryItem): Unit = {

    def fillSlotsWithSameItem(stackSize: Int, inventoryItem: InventoryItem): Int = {
      var leftQuantity = inventoryItem.quantity
      val slots = findItemsIndex(inventoryItem.itemId)
      for (slot <- slots if leftQuantity > 0) {
        leftQuantity = fillSlot(slot, leftQuantity, stackSize, inventoryItem.itemId)
      }
      leftQuantity
    }

    def fillFreeSlots(stackSize: Int, quantity: Int, inventoryItem: InventoryItem): Unit = {
      var leftQuantity = quantity
      val freeSlots = findAvailableIndex()
      for (slot <- freeSlots if leftQuantity > 0) {
        leftQuantity = fillSlot(slot, leftQuantity, stackSize, inventoryItem.itemId)
      }
    }

    val stackSize = Items.getStorableItemById(inventoryItem.itemId).stackSize
    val quantity = fillSlotsWithSameItem(stackSize, inventoryItem)
    fillFreeSlots(stackSize, quantity, inventoryItem)
  }

  def removeItem(slot: Int, inventoryItem: InventoryItem): Unit = {
    inventory(slot) match {
      case Some(item) if item.itemId == inventoryItem.itemId =>
        inventory(slot).get.quantity -= inventoryItem.quantity
        if (item.quantity <= 0)
          inventory(slot) = None
    }
  }

  def moveItem(from: Int, to: Int, quantity: Int): Unit = {
    inventory(from) match {
      case Some(fromItem) =>
        val quantityToBeMoved = math.min(fromItem.quantity, quantity)
        val exceedQuantity = addItem(to, InventoryItem(fromItem.itemId, quantityToBeMoved))
        fromItem.quantity -= quantityToBeMoved - exceedQuantity
        if(fromItem.quantity == 0) {
          inventory(from) = None
        }
      case None =>
    }
  }

  def retrieveAllItems(): List[Option[InventoryItem]] = inventory.toList

  def findAvailableIndex(): List[Int]

  def findItemsIndex(itemId: Int): List[Int]
}

sealed trait CraftingInventoty
sealed trait ChestInventory

object PlayerInventory {
  def Id = 0  //PlayerInventory

  //All range boundaries are inclusive by protocol
  private[logic] def CrafitingOutputSlot = 0
  private[logic] def CraftingInputSlotRange = Range(1, 4)
  private[logic] def ArmorSlotRange = Range(5, 8)
  private[logic] def MainInventorySlotRange = Range(9, 35)
  private[logic] def HotBarSlotRange = Range(36, 44)
  private[logic] def OffhandSlot = 45
}

case class PlayerInventory() extends Inventory with CraftingInventoty {
  import PlayerInventory._

  override protected val inventory: Array[Option[InventoryItem]] = Array.fill(OffhandSlot + 1)(None: Option[InventoryItem])

  def findHeldItemId(hotSlot: Int): Option[Int] = {
    inventory(hotSlot + HotBarSlotRange.start).map(_.itemId)
  }

  def useOneHeldItem(hotSlot: Int): Option[Int] = {
    findHeldItemId(hotSlot) match {
      case id @ Some(itemId) =>
        removeItem(hotSlot + HotBarSlotRange.start, InventoryItem(itemId, 1))
        id
      case None => None
    }
  }

  override def findAvailableIndex(): List[Int] = {
    (for {
      i <- (HotBarSlotRange ++ MainInventorySlotRange)
      if inventory(i).isEmpty
    } yield i).toList
  }

  override def findItemsIndex(itemId: Int): List[Int] = {
    (for {
      i <- (HotBarSlotRange ++ MainInventorySlotRange)
      if inventory(i).isDefined && inventory(i).get.itemId == itemId
    } yield i).toList
  }
}

object Chest {
  private[logic] def ChestSlotRange = Range(0, 26)
  private[logic] def MainInventorySlotRange = Range(27, 53)
  private[logic] def HotBarSlotRange = Range(54, 62)
}

case class Chest() extends Inventory with ChestInventory {
  import Chest._

  override protected val inventory: Array[Option[InventoryItem]] = Array.fill(HotBarSlotRange.end + 1)(None: Option[InventoryItem])

  override def findAvailableIndex(): List[Int] = List()

  override def findItemsIndex(itemId: Int): List[Int] = List()
}

object LargeChest{
  private[logic] def ChestSlotRange = Range(0, 53)
  private[logic] def MainInventorySlotRange = Range(53, 80)
  private[logic] def HotBarSlotRange = Range(81, 89)
}

case class LargeChest() extends Inventory with ChestInventory {
  import LargeChest._

  override protected val inventory: Array[Option[InventoryItem]] = Array.fill(HotBarSlotRange.end + 1)(None: Option[InventoryItem])

  override def findAvailableIndex(): List[Int] = List()

  override def findItemsIndex(itemId: Int): List[Int] = List()
}

object CraftingTable {
  private[logic] def CrafitingOutputSlot = 0
  private[logic] def CraftingInputSlotRange = Range(1, 9)
  private[logic] def MainInventorySlotRange = Range(10, 36)
  private[logic] def HotBarSlotRange = Range(37, 45)
}
case class CraftingTable() extends Inventory with CraftingInventoty {
  import CraftingTable._

  override protected val inventory: Array[Option[InventoryItem]] = Array.fill(HotBarSlotRange.end + 1)(None: Option[InventoryItem])

  override def findAvailableIndex(): List[Int] = List()

  override def findItemsIndex(itemId: Int): List[Int] = List()
}


