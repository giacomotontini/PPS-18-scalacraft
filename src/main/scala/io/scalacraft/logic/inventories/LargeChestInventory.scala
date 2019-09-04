package io.scalacraft.logic.inventories

import io.scalacraft.logic.inventories.traits.InventoryWithPlayerInventory
import io.scalacraft.logic.inventories.traits.range.MainHotRange

object LargeChestInventory extends MainHotRange {

  private[logic] val ChestSlotRange = 0 to 53
  private[logic] val MainInventorySlotRange = 53 to 80
  private[logic] val HotBarSlotRange = 81 to 89

}

class LargeChestInventory(val id: Int) extends InventoryWithPlayerInventory {

  import LargeChestInventory._

  override protected val inventory: Array[Option[InventoryItem]] = Array.fill(HotBarSlotRange.end + 1)(None)
  override protected[inventories] val mainHotInventoryRange: MainHotRange = LargeChestInventory
  override def inventoryClosed(): Any = {}

}