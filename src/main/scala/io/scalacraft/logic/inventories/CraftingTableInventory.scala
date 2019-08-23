package io.scalacraft.logic.inventories

import io.scalacraft.logic.inventories.traits.range.{CraftingRange, MainHotRange}
import io.scalacraft.logic.inventories.traits.{InventoryWithCrafting, InventoryWithPlayerInventory}

case class CraftingTableInventory(id: Int) extends InventoryWithPlayerInventory with InventoryWithCrafting {
  import CraftingTableInventory._

  override protected val inventory: Array[Option[InventoryItem]] = Array.fill(HotBarSlotRange.end + 1)(None: Option[InventoryItem])
  override protected[inventories] val mainHotInventoryRange: MainHotRange = CraftingTableInventory
  override protected[inventories] val craftingRange: CraftingRange = CraftingTableInventory
}

object CraftingTableInventory extends MainHotRange with CraftingRange {
  private[logic] def CraftingOutputSlot = 0
  private[logic] def CraftingInputSlotRange = Range(1, 9).inclusive
  private[logic] def MainInventorySlotRange = Range(10, 36).inclusive
  private[logic] def HotBarSlotRange = Range(37, 45).inclusive
}