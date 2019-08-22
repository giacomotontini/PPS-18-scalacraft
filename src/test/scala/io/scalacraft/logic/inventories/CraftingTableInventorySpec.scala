package io.scalacraft.logic.inventories

import io.scalacraft.logic.traits.inventories.{InventoryWithCraftingSpec, InventoryWithPlayerInventorySpec}

class CraftingTableInventorySpec extends InventoryWithPlayerInventorySpec[CraftingTableInventory]
  with InventoryWithCraftingSpec[CraftingTableInventory] {

  override def beforeEach(): Unit = {
    inventory = CraftingTableInventory(1) //dummyId
  }
}
