package io.scalacraft.logic

import java.util

import io.scalacraft.logic.inventories.{CraftingTableInventory, InventoryItem, PlayerInventory}
import org.scalatest.{FlatSpec, Matchers}

class InventorySpec extends FlatSpec with Matchers {

  "An inventory" should " start empty" in {
    val inventory = PlayerInventory()
    inventory.retrieveAllItems() should contain only (None)
  }

  "An inventory" should " allow a player to add items and retrieve them" in {
    val inventory = PlayerInventory()
    val item = InventoryItem(1, 5)
    inventory.addItem(0, item)
    inventory.retrieveAllItems().head.get shouldBe (item)
  }

  "An inventory" should " deny a player to add items in a full slot" in {
    val inventory = PlayerInventory()
    val item = InventoryItem(10, 64)
    inventory.addItem(0, item) shouldBe 0
    inventory.addItem(0, item) shouldBe 64
  }

  "An inventory" should "allow a player to remove an items" in {
    val inventory = PlayerInventory()
    val item = InventoryItem(1, 5)
    inventory.addItem(0, item)
    inventory.retrieveAllItems().head.get shouldBe (item)
    inventory.removeItem(0, item)
    inventory.retrieveAllItems() should contain only (None)
  }

  "An inventory" should "allow a player to add different items in different slots" in {
    val inventory = PlayerInventory()
    val item1 = InventoryItem(1, 10)
    val item2 = InventoryItem(2, 10)
    inventory.addItem(item1)
    inventory.addItem(item2)
    inventory.retrieveAllItems() should contain allOf(Some(item1), Some(item2))
  }

  "An inventory" should "stack items of the same type (if possible according to stack size)" in {
    val inventory = PlayerInventory()
    val dirtBlock = InventoryItem(10, 30) //dirt has a stack size of 64 and has id = 10
    inventory.addItem(dirtBlock)
    inventory.addItem(dirtBlock)
    inventory.retrieveAllItems() should contain(Some(InventoryItem(10, 60)))
  }

  "An inventory" should "stack items of the same type which exceed stack size correctly " in {
    val inventory = PlayerInventory()
    val dirtBlock = InventoryItem(10, 65) //dirt has a stack size of 64 and has id = 10
    inventory.addItem(dirtBlock)
    inventory.retrieveAllItems() should contain(Some(InventoryItem(10, 64)))
    inventory.retrieveAllItems() should contain(Some(InventoryItem(10, 1)))
  }

  "An inventory" should " allow to move items" in {
    val inventory = PlayerInventory()
    val dirtBlock = InventoryItem(10, 64)
    inventory.addItem(0, dirtBlock)
    inventory.moveItem(0, 1, 32)
    inventory.retrieveAllItems().head shouldBe (Some(InventoryItem(10, 32)))
    inventory.retrieveAllItems()(1) shouldBe (Some(InventoryItem(10, 32)))
  }

  "An inventory" should " allow to move items and stack them" in {
    val inventory = PlayerInventory()
    val dirtBlock = InventoryItem(10, 32)
    inventory.addItem(0, dirtBlock)
    inventory.addItem(1, dirtBlock)
    inventory.moveItem(0, 1, 32)
    inventory.retrieveAllItems().head shouldBe (None)
    inventory.retrieveAllItems()(1) shouldBe (Some(InventoryItem(10, 64)))
  }

  "An inventory" should " allow to move items and stack them if possible accordingly to stack size" in {
    val inventory = PlayerInventory()
    val dirtBlock = InventoryItem(10, 31)
    val dirtBlock1 = InventoryItem(10, 34) //stack size for dirt(10) is 64
    inventory.addItem(0, dirtBlock)
    inventory.addItem(1, dirtBlock1)
    inventory.moveItem(0, 1, 31)
    inventory.retrieveAllItems().head shouldBe (Some(InventoryItem(10, 1)))
    inventory.retrieveAllItems()(1) shouldBe (Some(InventoryItem(10, 64)))
  }

  "A player inventory" should " allow to identify held item correctly" in {
    val inventory = PlayerInventory()
    inventory.addItem(PlayerInventory.HotBarSlotRange.end, InventoryItem(itemId = 1, quantity = 5))
    inventory.findHeldItemId(0) shouldBe (None)
    inventory.findHeldItemId(8) shouldBe (Some(1)) //hotbar has 9 slots from 0 to 8 (inclusive)
  }

  "A player inventory" should " be empty after using an held item" in {
    val inventory = PlayerInventory()
    inventory.addItem(PlayerInventory.HotBarSlotRange.start, InventoryItem(itemId = 1, quantity = 1))
    inventory.useOneHeldItem(0) //hotbar has 9 slots from 0 to 8 (inclusive)
    inventory.retrieveAllItems() should contain only None
  }

  "An inventory with player inventory section" should " be filled correctly when a player inventory is given" in {
    val inventory = PlayerInventory()
    val playerInventorySectionSlots = PlayerInventory.MainInventorySlotRange ++ PlayerInventory.HotBarSlotRange
    val items = (playerInventorySectionSlots map (i => Some(InventoryItem(i, 1)))).toList
    inventory.addPlayerInventory(items)
    inventory.retrieveInventoryItems shouldBe items
    inventory.retrieveAllItems().zipWithIndex.filterNot(i => playerInventorySectionSlots.contains(i._2)).map(_._1) should contain only None
  }


  "An inventory with crafting section" should " give items housed in crafting section" in {
    val inventory = CraftingTableInventory(id = 1) //dummyId
    val craftingSectionSlots = CraftingTableInventory.CraftingInputSlotRange
    craftingSectionSlots.foreach(inventory.addItem(_, InventoryItem(1, 1)))
    val craftingItems = inventory.retrieveCraftingItems()
    craftingItems should contain only Some(InventoryItem(1, 1))
    craftingItems.size shouldBe craftingSectionSlots.length
  }

  "An inventory with crafting section" should " clear output slot and remove used ingredients when a crafted object is accepted" in {
    val inventory = CraftingTableInventory(id = 1) //dummyId
    val craftingSectionSlots = CraftingTableInventory.CraftingInputSlotRange
    val crafted = InventoryItem(5, 10) //dummy crafted
    craftingSectionSlots.foreach(inventory.addItem(_, InventoryItem(1, 2)))
    inventory.addCrafted(crafted)
    inventory.craftingAccepted()
    inventory.retrieveCraftingItems() should contain only Some(InventoryItem(1, 1))
    inventory.retrieveAllItems()(CraftingTableInventory.CraftingOutputSlot) shouldBe None
  }

  "An inventory with crafting section when closed" should "move back items into player inventory section" in {
    val inventory = CraftingTableInventory(id = 1) //dummyId
    val craftingSectionSlots = CraftingTableInventory.CraftingInputSlotRange
    inventory.addItem(craftingSectionSlots.start, InventoryItem(1, 1))
    inventory.inventoryClosed()
    inventory.retrieveCraftingItems() should contain only None
    inventory.retrieveInventoryItems().contains(Some(InventoryItem(1,1))) shouldBe true
  }
}
