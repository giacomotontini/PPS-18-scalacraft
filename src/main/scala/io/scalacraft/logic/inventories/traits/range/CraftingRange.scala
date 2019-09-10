package io.scalacraft.logic.inventories.traits.range

/**
 * Keeps all ranges involved in a crafting inventory
 */
trait CraftingRange {

  private[logic] val CraftingInputSlotRange: Range
  private[logic] val CraftingOutputSlot: Int

}
