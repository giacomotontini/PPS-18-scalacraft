package io.scalacraft.logic.inventories.traits.range

trait CraftingRange {
  private[logic] def CraftingInputSlotRange: Range
  private[logic] def CraftingOutputSlot: Int
}
