package io.scalacraft.logic.inventories

import io.scalacraft.logic.inventories.traits.InventoryWithPlayerInventory
import io.scalacraft.logic.inventories.traits.range.MainHotRange

class ChestInventory(val id: Int) extends InventoryWithPlayerInventory {

  import ChestInventory._

  override protected val inventory: Array[Option[InventoryItem]] = Array.fill(HotBarSlotRange.end + 1)(None)
  override protected[inventories] val mainHotInventoryRange: MainHotRange = ChestInventory

  override def inventoryClosed(): Any = {}
}

object ChestInventory extends MainHotRange {

  private[logic] val ChestSlotRange = 0 to 26
  private[logic] val MainInventorySlotRange = 27 to 53
  private[logic] val HotBarSlotRange = 54 to 62

}
