package io.scalacraft.logic


case class InventoryItem(itemId: Int, var quantity: Int = 0)

sealed trait Inventory {
  protected val inventory: Array[Option[InventoryItem]]

  def addItem(inventoryItem: InventoryItem): Unit = {
    val slots = findItemsIndex(inventoryItem.itemId)
    if(slots.isEmpty) {
      val freeSlots = findAvailableIndex()
      if(!freeSlots.isEmpty) {
        inventory(freeSlots.head) = Some(inventoryItem)
      }
    } else {
      inventory(slots.head).get.quantity += inventoryItem.quantity
    }

  }

  def removeItem(slot: Int, inventoryItem: InventoryItem): Unit = {
    if (inventory(slot).isDefined && inventory(slot).get.itemId == inventoryItem.itemId) {
      inventory(slot).get.quantity -= inventoryItem.quantity
      if (inventory(slot).get.quantity <= 0)
        inventory(slot) = None
    }
  }

  def moveItem(from: Int, to: Int, quantity: Int): Unit = {
    if (inventory(from).isDefined && inventory(from).get.quantity >= quantity) {
      inventory(from).get.quantity -= quantity
      if (inventory(to).isDefined) {
        inventory(to).get.quantity += quantity
      } else {
        inventory(to) = Some(InventoryItem(inventory(from).get.itemId, quantity))
      }
    }
  }

  def retrieveAllItems(): List[Option[InventoryItem]] = {
    inventory.toList
  }

  def findAvailableIndex(): List[Int]

  def findItemsIndex(itemId: Int): List[Int]
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

  override protected val inventory: Array[Option[InventoryItem]] = Array.fill(OffhandSlot + 1)(None:Option[InventoryItem])

  def findHeldedItemId(hotSlot: Int): Option[Int] = {
    inventory(hotSlot + HotBarSlotRange.start).map(_.itemId)
  }

  def removeUsedHeldedItem(hotSlot: Int): Int = {
    findHeldedItemId(hotSlot) match {
      case Some(itemId) =>
        removeItem(hotSlot + HotBarSlotRange.start, InventoryItem(itemId, 1))
        itemId
      case None => 0
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

case class Chest() extends Inventory with ChestInventory {

  private val ChestSlotRange = Range(0, 26)
  private val MainInventorySlotRange = Range(27, 53)
  private val HotBarSlotRange = Range(54, 62)
  override protected val inventory: Array[Option[InventoryItem]] = Array.fill(HotBarSlotRange.end + 1)(None:Option[InventoryItem])

  override def findAvailableIndex(): List[Int] = List()
  override def findItemsIndex(itemId: Int): List[Int] = List()
}

case class LargeChest() extends Inventory with ChestInventory {

  private val ChestSlotRange = Range(0, 53)
  private val MainInventorySlotRange = Range(53, 80)
  private val HotBarSlotRange = Range(81, 89)
  override protected val inventory: Array[Option[InventoryItem]] = Array.fill(HotBarSlotRange.end + 1)(None:Option[InventoryItem])

  override def findAvailableIndex(): List[Int] = List()
  override def findItemsIndex(itemId: Int): List[Int] = List()
}

case class CraftingTable() extends Inventory with CraftingInventoty {

  private val CrafitingOutputSlot = 0
  private val CraftingInputSlotRange = Range(1,9)
  private val MainInventorySlotRange = Range(10, 36)
  private val HotBarSlotRange = Range(37, 45)
  override protected val inventory: Array[Option[InventoryItem]] = Array.fill(HotBarSlotRange.end + 1)(None:Option[InventoryItem])

  override def findAvailableIndex(): List[Int] = List()
  override def findItemsIndex(itemId: Int): List[Int] = List()
}


