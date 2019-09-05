package io.scalacraft.logic.inventories.traits.range

/**
 * Keep all ranges in a base inventory.
 */
trait MainHotRange {

  private[logic] val MainInventorySlotRange: Range
  private[logic] val HotBarSlotRange: Range

}
