package io.scalacraft.logic.inventories

import io.scalacraft.logic.inventories.traits.{InventoryWithCraftingSpec, InventoryWithPlayerInventorySpec}
import io.scalacraft.logic.inventories.traits.InventoryWithCraftingSpec

class CraftingTableInventorySpec extends InventoryWithPlayerInventorySpec[CraftingTableInventory]
  with InventoryWithCraftingSpec[CraftingTableInventory] {

  override def beforeEach(): Unit = {
    inventory = new CraftingTableInventory(1) //dummyId
  }
}
