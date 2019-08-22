package io.scalacraft.logic.inventories

import io.scalacraft.logic.inventories.traits.InventoryWithPlayerInventory
import io.scalacraft.logic.inventories.traits.range.MainHotRange

object LargeChestInventory extends MainHotRange {
  private[logic] def ChestSlotRange = Range(0, 53).inclusive
  private[logic] def MainInventorySlotRange = Range(53, 80).inclusive
  private[logic] def HotBarSlotRange = Range(81, 89).inclusive
}

case class LargeChestInventory(id: Int) extends InventoryWithPlayerInventory {
  import LargeChestInventory._

  override protected val inventory: Array[Option[InventoryItem]] = Array.fill(HotBarSlotRange.end + 1)(None: Option[InventoryItem])
  override protected[inventories] val mainHotInventoryRange: MainHotRange = LargeChestInventory
  override def inventoryClosed(): Any = {}

}