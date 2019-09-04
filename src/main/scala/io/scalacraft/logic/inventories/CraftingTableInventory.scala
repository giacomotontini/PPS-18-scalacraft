package io.scalacraft.logic.inventories

import io.scalacraft.logic.inventories.traits.range.{CraftingRange, MainHotRange}
import io.scalacraft.logic.inventories.traits.{InventoryWithCrafting, InventoryWithPlayerInventory}

class CraftingTableInventory(val id: Int) extends InventoryWithPlayerInventory with InventoryWithCrafting {

  import CraftingTableInventory._

  override protected val inventory: Array[Option[InventoryItem]] = Array.fill(HotBarSlotRange.end + 1)(None)
  override protected[inventories] val mainHotInventoryRange: MainHotRange = CraftingTableInventory
  override protected[inventories] val craftingRange: CraftingRange = CraftingTableInventory

}

object CraftingTableInventory extends MainHotRange with CraftingRange {

  private[logic] val CraftingOutputSlot = 0
  private[logic] val CraftingInputSlotRange = 1 to 9
  private[logic] val MainInventorySlotRange = 10 to 36
  private[logic] val HotBarSlotRange = 37 to 45

}