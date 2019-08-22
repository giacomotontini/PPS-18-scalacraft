package io.scalacraft.logic.inventories

import io.scalacraft.logic.traits.inventories._
import io.scalacraft.logic.traits.inventories.range.{CraftingRange, MainHotRange}

case class CraftingTableInventory(id: Int) extends InventoryWithPlayerInventory with InventoryWithCrafting {
  import CraftingTableInventory._

  override protected val inventory: Array[Option[InventoryItem]] = Array.fill(HotBarSlotRange.end + 1)(None: Option[InventoryItem])
  override protected val mainHotInventoryRange: MainHotRange = CraftingTableInventory
  override protected val craftingRange: CraftingRange = CraftingTableInventory
}

object CraftingTableInventory extends MainHotRange with CraftingRange {
  private[logic] def CraftingOutputSlot = 0
  private[logic] def CraftingInputSlotRange = Range(1, 9).inclusive
  private[logic] def MainInventorySlotRange = Range(10, 36).inclusive
  private[logic] def HotBarSlotRange = Range(37, 45).inclusive
}