package io.scalacraft.logic.traits.inventories.range

trait CraftingRange {
  private[logic] def CraftingInputSlotRange: Range
  private[logic] def CraftingOutputSlot: Int
}
