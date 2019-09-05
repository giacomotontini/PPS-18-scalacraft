package io.scalacraft.logic.inventories.traits

import io.scalacraft.logic.inventories.InventoryItem
import io.scalacraft.logic.inventories.traits.range.CraftingRange

/**
 * Represent an inventory with crafting zone
 */
trait InventoryWithCrafting extends Inventory {

  protected[inventories] val craftingRange: CraftingRange

  /**
   * @return a list of all the items on the crafting input zone.
   */
  def retrieveCraftingItems: List[Option[InventoryItem]] =
    inventory.slice(craftingRange.CraftingInputSlotRange.start, craftingRange.CraftingInputSlotRange.end + 1).toList

  /**
   * Add an item to the crafting output slot.
   * @param item the item crafted
   */
  def addCrafted(item: InventoryItem): Unit = {
    clearSlot(craftingRange.CraftingOutputSlot)
    addItem(craftingRange.CraftingOutputSlot, item)
  }

  /**
   * Clear the crafting output slot.
   */
  def clearCrafted(): Unit = clearSlot(craftingRange.CraftingOutputSlot)

  /**
   * Determine the acceptance of the crafted item by the user.
   * Clear the crafting output slot and remove the used item from the crafting input area.
   */
  def craftingAccepted(): Unit = {
    clearCrafted()
    craftingRange.CraftingInputSlotRange.foreach(removeItem(_, 1))
  }

  /**
   * When an inventory with crafting is closed, all the items on the crafting input area are moved back
   * the the player inventory side and the crafting output cleared (Recipe refused).
   */
  def inventoryClosed(): Unit = retrieveCraftingItems.zip(craftingRange.CraftingInputSlotRange) foreach {
    case (Some(item), index) =>
      clearSlot(index)
      addItem(item) //cannot be a move, need to find free slots or group with other
    case _ =>
  }

}
