package io.scalacraft.logic.traits.inventories.range

trait MainHotRange {
  private[logic] def MainInventorySlotRange: Range
  private[logic] def HotBarSlotRange: Range
}
