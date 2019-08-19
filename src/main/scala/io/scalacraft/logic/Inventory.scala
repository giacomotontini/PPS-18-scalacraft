package io.scalacraft.logic

import io.scalacraft.loaders.Items

case class InventoryItem(itemId: Int, var quantity: Int = 0) {

  def + (inventoryItem: InventoryItem): InventoryItem = {
    require(this.itemId == inventoryItem.itemId, "Sum of different inventory item not allowed.")
    InventoryItem(itemId, quantity + inventoryItem.quantity)
  }
}

trait MainHotRange {
  private[logic] def MainInventorySlotRange: Range
  private[logic] def HotBarSlotRange: Range
}
trait CraftingRange {
  private[logic] def CraftingInputSlotRange: Range
  private[logic] def CraftingOutputSlot: Int
}

trait Inventory {
  protected val inventory: Array[Option[InventoryItem]]
  protected val mainHotInventoryRange: MainHotRange

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

  def addItem(inventoryItem: InventoryItem): Int = {

    def fillSlotsWithSameItem(stackSize: Int, inventoryItem: InventoryItem): Int = {
      var leftQuantity = inventoryItem.quantity
      val slots = findItemsIndex(inventoryItem.itemId)
      for (slot <- slots if leftQuantity > 0) {
        leftQuantity = fillSlot(slot, leftQuantity, stackSize, inventoryItem.itemId)
      }
      leftQuantity
    }

    def fillFreeSlots(stackSize: Int, quantity: Int, inventoryItem: InventoryItem): Int = {
      var leftQuantity = quantity
      val freeSlots = findAvailableIndex()
      for (slot <- freeSlots if leftQuantity > 0) {
        leftQuantity = fillSlot(slot, leftQuantity, stackSize, inventoryItem.itemId)
      }
      leftQuantity
    }

    val stackSize = Items.getStorableItemById(inventoryItem.itemId).stackSize
    val quantity = fillSlotsWithSameItem(stackSize, inventoryItem)
    fillFreeSlots(stackSize, quantity, inventoryItem)
  }

  def removeItem(slot: Int, inventoryItem: InventoryItem): Unit = {
    removeItem(slot, inventoryItem.quantity)
  }

  def removeItem(slot: Int, quantity: Int): Unit = {
    inventory(slot) match {
      case Some(item) =>
        inventory(slot).get.quantity -= quantity
        if (item.quantity <= 0)
          clearSlot(slot)
      case None =>
    }
  }

  def clearSlot(slot: Int): Unit = {
    inventory(slot) = None
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

  //Retrieve all items, included non inventory one (like crafting output, armors etc.)
  def retrieveAllItems(): List[Option[InventoryItem]] = inventory.toList

  def retrieveInventoryItems(): List[Option[InventoryItem]] = {
    inventory.takeRight(mainHotInventoryRange.HotBarSlotRange.length + mainHotInventoryRange.MainInventorySlotRange.length).toList
  }

  def findAvailableIndex(): List[Int] = List()

  def findItemsIndex(itemId: Int): List[Int] = List()
}

trait InventoryWithPlayerInventory extends Inventory {
  //When an inventory with inventory section is opened, the player inventory should be moved here
  def addPlayerInventory(playerInventory: PlayerInventory): Unit = {
    playerInventory.retrieveInventoryItems()
      .zipWithIndex
      .foreach(i => inventory.update(i._2 + mainHotInventoryRange.MainInventorySlotRange.start, i._1))
  }
}

trait CraftingInventory extends Inventory {
 protected val craftingRange: CraftingRange

  def retrieveCraftingItems(): List[Option[InventoryItem]] = {
    inventory.slice(craftingRange.CraftingInputSlotRange.start, craftingRange.CraftingInputSlotRange.end + 1).toList
  }

  def addCrafted(item: InventoryItem): Unit = {
    clearSlot(craftingRange.CraftingOutputSlot)
    addItem(craftingRange.CraftingOutputSlot, item)
  }

  def craftingAccepted(): Unit = {
    craftingRange.CraftingInputSlotRange.foreach (removeItem(_, 1))
  }

  def inventoryClosed(): Unit = {
    retrieveCraftingItems().collect { case Some(item) => addItem(item) }
    craftingRange.CraftingInputSlotRange.foreach(_ => inventory(_) = None)
  }
}

object PlayerInventory extends MainHotRange with CraftingRange {
  def Id = 0  //PlayerInventory
  //All range boundaries are inclusive by protocol
  private[logic] def CraftingOutputSlot = 0
  private[logic] def CraftingInputSlotRange = Range(1, 4).inclusive
  private[logic] def ArmorSlotRange = Range(5, 8).inclusive
  private[logic] def MainInventorySlotRange = Range(9, 35).inclusive
  private[logic] def HotBarSlotRange = Range(36, 44).inclusive
  private[logic] def OffhandSlot = 45
}

case class PlayerInventory() extends CraftingInventory {
  import PlayerInventory._

  override protected val inventory: Array[Option[InventoryItem]] = Array.fill(OffhandSlot + 1)(None: Option[InventoryItem])
  override protected val mainHotInventoryRange: MainHotRange = PlayerInventory
  override protected val craftingRange: CraftingRange = PlayerInventory

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

  override def retrieveInventoryItems(): List[Option[InventoryItem]] = {
    inventory.dropRight(1).takeRight(HotBarSlotRange.length + MainInventorySlotRange.length).toList
  }


}

object Chest extends MainHotRange {
  private[logic] def ChestSlotRange = Range(0, 26).inclusive
  private[logic] def MainInventorySlotRange = Range(27, 53).inclusive
  private[logic] def HotBarSlotRange = Range(54, 62).inclusive
}

case class Chest() extends Inventory {
  import Chest._

  override protected val inventory: Array[Option[InventoryItem]] = Array.fill(HotBarSlotRange.end)(None: Option[InventoryItem])
  override protected val mainHotInventoryRange: MainHotRange = Chest

}

object LargeChest extends MainHotRange {
  private[logic] def ChestSlotRange = Range(0, 53).inclusive
  private[logic] def MainInventorySlotRange = Range(53, 80).inclusive
  private[logic] def HotBarSlotRange = Range(81, 89).inclusive
}
case class LargeChest() extends Inventory {
  import LargeChest._

  override protected val inventory: Array[Option[InventoryItem]] = Array.fill(HotBarSlotRange.end)(None: Option[InventoryItem])
  override protected val mainHotInventoryRange: MainHotRange = LargeChest
}

object CraftingTable extends MainHotRange {
  private[logic] def CraftingOutput = 0
  private[logic] def CraftingInputSlotRange = Range(1, 9).inclusive
  private[logic] def MainInventorySlotRange = Range(10, 36).inclusive
  private[logic] def HotBarSlotRange = Range(37, 45).inclusive
}
case class CraftingTable() extends Inventory {
  import CraftingTable._

  override protected val inventory: Array[Option[InventoryItem]] = Array.fill(HotBarSlotRange.end)(None: Option[InventoryItem])
  override protected val mainHotInventoryRange: MainHotRange = CraftingTable

}


