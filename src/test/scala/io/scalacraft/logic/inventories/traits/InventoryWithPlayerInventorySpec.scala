package io.scalacraft.logic.inventories.traits

import io.scalacraft.logic.inventories.InventoryItem

trait InventoryWithPlayerInventorySpec[T <: InventoryWithPlayerInventory] extends InventorySpec[T] {

  "An inventory with player inventory section" should " be filled correctly when a player inventory is given" in {
    val playerInventorySectionSlots = inventory.mainHotInventoryRange.MainInventorySlotRange ++ inventory.mainHotInventoryRange.HotBarSlotRange
    val items = (playerInventorySectionSlots map (i => Some(InventoryItem(i, 1)))).toList
    inventory.addPlayerInventory(items)
    inventory.retrieveInventoryItems shouldBe items
    inventory.retrieveAllItems.zipWithIndex.filterNot(i => playerInventorySectionSlots.contains(i._2)).map(_._1) should contain only None
  }

}
