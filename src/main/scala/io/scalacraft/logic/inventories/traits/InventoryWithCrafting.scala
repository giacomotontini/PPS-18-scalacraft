package io.scalacraft.logic.inventories.traits

import io.scalacraft.logic.inventories.InventoryItem
import io.scalacraft.logic.inventories.traits.range.CraftingRange

trait InventoryWithCrafting extends Inventory {
 protected[inventories] val craftingRange: CraftingRange

  def retrieveCraftingItems(): List[Option[InventoryItem]] = {
    inventory.slice(craftingRange.CraftingInputSlotRange.start, craftingRange.CraftingInputSlotRange.end + 1).toList
  }

  def addCrafted(item: InventoryItem): Unit = {
    clearSlot(craftingRange.CraftingOutputSlot)
    addItem(craftingRange.CraftingOutputSlot, item)
  }

  def clearCrafted(): Unit = {
    clearSlot(craftingRange.CraftingOutputSlot)
  }

  def craftingAccepted(): Unit = {
    clearCrafted()
    craftingRange.CraftingInputSlotRange.foreach (removeItem(_, 1))

  }

  def inventoryClosed(): Unit = {
    retrieveCraftingItems().zip(craftingRange.CraftingInputSlotRange).foreach {
      case (Some(item), index) =>
        clearSlot(index)
        addItem(item) //cannot be a move, need to find free slots or group with other
      case _ =>
    }
  }
}
