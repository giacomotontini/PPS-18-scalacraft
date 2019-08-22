package io.scalacraft.logic.inventories.traits.range

trait MainHotRange {
  private[logic] def MainInventorySlotRange: Range
  private[logic] def HotBarSlotRange: Range
}
