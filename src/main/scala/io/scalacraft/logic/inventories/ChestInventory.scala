package io.scalacraft.logic.inventories

import io.scalacraft.logic.inventories.traits.InventoryWithPlayerInventory
import io.scalacraft.logic.inventories.traits.range.MainHotRange

/**
 * Represent a minecraft chest (normal size) inventory.
 * @param id the id of the opened window.
 */
class ChestInventory(val id: Int) extends InventoryWithPlayerInventory {

  import ChestInventory._

  override protected val inventory: Array[Option[InventoryItem]] = Array.fill(HotBarSlotRange.end + 1)(None)
  override protected[inventories] val mainHotInventoryRange: MainHotRange = ChestInventory

  /**
   * Does nothing on chest.
   */
  override def inventoryClosed(): Unit = {}
}

object ChestInventory extends MainHotRange {
  //All range's boundaries are inclusive by protocol
  private[logic] val ChestSlotRange = 0 to 26
  private[logic] val MainInventorySlotRange = 27 to 53
  private[logic] val HotBarSlotRange = 54 to 62

}
