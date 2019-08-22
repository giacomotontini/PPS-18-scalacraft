package io.scalacraft.logic.traits.inventories

import io.scalacraft.logic.inventories.InventoryItem

trait InventoryWithPlayerInventory extends Inventory {
  //When an inventory with inventory section is opened, the player inventory's items should be moved here
  def addPlayerInventory(playerInventoryItems: List[Option[InventoryItem]]): Unit = {
    playerInventoryItems.zipWithIndex.foreach(item => inventory.update(item._2 + mainHotInventoryRange.MainInventorySlotRange.start, item._1))
  }

  override def findAvailableIndex(): List[Int] = {
    (for {
      i <- (mainHotInventoryRange.HotBarSlotRange ++ mainHotInventoryRange.MainInventorySlotRange)
      if inventory(i).isEmpty
    } yield i).toList
  }

  override def findItemsIndex(itemId: Int): List[Int] = {
    (for {
      i <- (mainHotInventoryRange.HotBarSlotRange ++ mainHotInventoryRange.MainInventorySlotRange)
      if inventory(i).isDefined && inventory(i).get.itemId == itemId
    } yield i).toList
  }
}
