package io.scalacraft.logic.traits.inventories

import io.scalacraft.logic.inventories.InventoryItem


trait InventoryWithCraftingSpec[T <: InventoryWithCrafting] extends InventorySpec[T] {

  "An inventory with crafting section" should " give items housed in crafting section" in {
    val craftingSectionSlots = inventory.craftingRange.CraftingInputSlotRange
    craftingSectionSlots.foreach(inventory.addItem(_, InventoryItem(1, 1)))
    val craftingItems = inventory.retrieveCraftingItems()
    craftingItems should contain only Some(InventoryItem(1, 1))
    craftingItems.size shouldBe craftingSectionSlots.length
  }

  it should " clear output slot and remove used ingredients when a crafted object is accepted" in {
    val craftingSectionSlots = inventory.craftingRange.CraftingInputSlotRange
    val crafted = InventoryItem(5, 10) //dummy crafted
    craftingSectionSlots.foreach(inventory.addItem(_, InventoryItem(1, 2)))
    inventory.addCrafted(crafted)
    inventory.craftingAccepted()
    inventory.retrieveCraftingItems() should contain only Some(InventoryItem(1, 1))
    inventory.retrieveAllItems()(inventory.craftingRange.CraftingOutputSlot) shouldBe None
  }

  it should "move back items into player inventory section when closed" in {
    val craftingSectionSlots = inventory.craftingRange.CraftingInputSlotRange
    inventory.addItem(craftingSectionSlots.start, InventoryItem(1, 1))
    inventory.inventoryClosed()
    inventory.retrieveCraftingItems() should contain only None
    inventory.retrieveInventoryItems().contains(Some(InventoryItem(1,1))) shouldBe true
  }
}
