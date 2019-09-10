package io.scalacraft.logic.inventories

import io.scalacraft.logic.inventories.traits.{InventoryWithCraftingSpec, InventoryWithPlayerInventorySpec}
import io.scalacraft.logic.inventories.traits.InventoryWithCraftingSpec

class PlayerInventorySpec extends InventoryWithPlayerInventorySpec[PlayerInventory]
  with InventoryWithCraftingSpec[PlayerInventory] {

  override def beforeEach(): Unit = {
    inventory = new PlayerInventory
  }

  "A player inventory" should " allow to identify held item correctly" in {
    inventory.addItem(PlayerInventory.HotBarSlotRange.end, InventoryItem(itemId = 1, quantity = 5))
    inventory.findHeldItemId(0) shouldBe None
    inventory.findHeldItemId(8) shouldBe Some(1) //hotbar has 9 slots from 0 to 8 (inclusive)
  }

  it should " be empty after using an held item" in {
    inventory.addItem(PlayerInventory.HotBarSlotRange.start, InventoryItem(itemId = 1, quantity = 1))
    inventory.useOneHeldItem(0) //hotbar has 9 slots from 0 to 8 (inclusive)
    inventory.retrieveAllItems should contain only None
  }
}
