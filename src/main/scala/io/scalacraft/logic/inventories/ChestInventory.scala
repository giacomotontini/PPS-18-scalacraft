package io.scalacraft.logic.inventories

import io.scalacraft.logic.inventories.traits.InventoryWithPlayerInventory
import io.scalacraft.logic.inventories.traits.range.MainHotRange

case class ChestInventory(id: Int) extends InventoryWithPlayerInventory {
  import ChestInventory._

  override protected val inventory: Array[Option[InventoryItem]] = Array.fill(HotBarSlotRange.end + 1)(None: Option[InventoryItem])
  override protected[inventories] val mainHotInventoryRange: MainHotRange = ChestInventory
  override def inventoryClosed(): Any = {}
}

object ChestInventory extends MainHotRange {
  private[logic] def ChestSlotRange = Range(0, 26).inclusive
  private[logic] def MainInventorySlotRange = Range(27, 53).inclusive
  private[logic] def HotBarSlotRange = Range(54, 62).inclusive
}