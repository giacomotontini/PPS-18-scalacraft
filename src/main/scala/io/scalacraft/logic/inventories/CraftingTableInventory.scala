package io.scalacraft.logic.inventories

import io.scalacraft.logic.inventories.traits.range.{CraftingRange, MainHotRange}
import io.scalacraft.logic.inventories.traits.{InventoryWithCrafting, InventoryWithPlayerInventory}

/**
 * Represent a minecraft crafting table inventory.
 *
 * @param id the id of the opened window.
 */
class CraftingTableInventory(val id: Int) extends InventoryWithPlayerInventory with InventoryWithCrafting {

  import CraftingTableInventory._

  override protected val inventory: Array[Option[InventoryItem]] = Array.fill(HotBarSlotRange.end + 1)(None)
  override protected[inventories] val mainHotInventoryRange: MainHotRange = CraftingTableInventory
  override protected[inventories] val craftingRange: CraftingRange = CraftingTableInventory

}

object CraftingTableInventory extends MainHotRange with CraftingRange {
  //All range's boundaries are inclusive by protocol
  private[logic] val CraftingOutputSlot = 0
  private[logic] val CraftingInputSlotRange = 1 to 9
  private[logic] val MainInventorySlotRange = 10 to 36
  private[logic] val HotBarSlotRange = 37 to 45

}