package io.scalacraft.logic.inventories

import io.scalacraft.logic.inventories.traits.InventoryWithPlayerInventory
import io.scalacraft.logic.inventories.traits.range.MainHotRange

/**
 * Represent a minecraft chest (large size) inventory.
 *
 * @param id the id of the opened window.
 */
class LargeChestInventory(val id: Int) extends InventoryWithPlayerInventory {

  import LargeChestInventory._

  override protected val inventory: Array[Option[InventoryItem]] = Array.fill(HotBarSlotRange.end + 1)(None)
  override protected[inventories] val mainHotInventoryRange: MainHotRange = LargeChestInventory

  /**
   * Does nothing on chest.
   */
  override def inventoryClosed(): Unit = {}

}

object LargeChestInventory extends MainHotRange {
  //All range's boundaries are inclusive by protocol
  private[logic] val ChestSlotRange = 0 to 53
  private[logic] val MainInventorySlotRange = 53 to 80
  private[logic] val HotBarSlotRange = 81 to 89

}