package io.scalacraft.logic.inventories.traits

import io.scalacraft.logic.inventories.InventoryItem

/**
 * An inventory with a player inventory area.
 */
trait InventoryWithPlayerInventory extends Inventory {

  /**
   * When an inventory with a player inventory section is opened, the player inventory's items should be moved here
   * Add the player's inventory items to the current inventory.
   * @param playerInventoryItems the player inventory items to be moved in the opened inventory
   */
  def addPlayerInventory(playerInventoryItems: List[Option[InventoryItem]]): Unit =
    playerInventoryItems.zipWithIndex foreach { item =>
      inventory.update(item._2 + mainHotInventoryRange.MainInventorySlotRange.start, item._1)
    }

  override def findAvailableIndex: List[Int] = (for {
    i <- mainHotInventoryRange.HotBarSlotRange ++ mainHotInventoryRange.MainInventorySlotRange
    if inventory(i).isEmpty
  } yield i).toList

  override def findItemsIndex(itemId: Int): List[Int] = (for {
    i <- mainHotInventoryRange.HotBarSlotRange ++ mainHotInventoryRange.MainInventorySlotRange
    if inventory(i).isDefined && inventory(i).get.itemId == itemId
  } yield i).toList

}
