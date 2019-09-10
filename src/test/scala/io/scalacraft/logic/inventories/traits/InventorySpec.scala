package io.scalacraft.logic.inventories.traits

import io.scalacraft.logic.inventories.InventoryItem
import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers}


trait InventorySpec[T <: Inventory] extends FlatSpec with Matchers with BeforeAndAfterEach {

  var inventory: T = _

  "An inventory" should " start empty" in {
    inventory.retrieveAllItems should contain only (None)
  }

  it should " allow a player to add items and retrieve them" in {
    val item = InventoryItem(1, 5)
    inventory.addItem(0, item)
    inventory.retrieveAllItems.head.get shouldBe (item)
  }

  it should " deny a player to add items in a full slot" in {
    val item = InventoryItem(10, 64)
    inventory.addItem(0, item) shouldBe 0
    inventory.addItem(0, item) shouldBe 64
  }

  it should "allow a player to remove an items" in {
    val item = InventoryItem(1, 5)
    inventory.addItem(0, item)
    inventory.retrieveAllItems.head.get shouldBe (item)
    inventory.removeItem(0, item)
    inventory.retrieveAllItems should contain only (None)
  }

  it should "allow a player to add different items in different slots" in {
    val item1 = InventoryItem(1, 10)
    val item2 = InventoryItem(2, 10)
    inventory.addItem(item1)
    inventory.addItem(item2)
    inventory.retrieveAllItems should contain allOf(Some(item1), Some(item2))
  }

  it should "stack items of the same type (if possible according to stack size)" in {
    val dirtBlock = InventoryItem(10, 30) //dirt has a stack size of 64 and has id = 10
    inventory.addItem(dirtBlock)
    inventory.addItem(dirtBlock)
    inventory.retrieveAllItems should contain(Some(InventoryItem(10, 60)))
  }

  it should "stack items of the same type which exceed stack size correctly " in {
    val dirtBlock = InventoryItem(10, 65) //dirt has a stack size of 64 and has id = 10
    inventory.addItem(dirtBlock)
    inventory.retrieveAllItems should contain(Some(InventoryItem(10, 64)))
    inventory.retrieveAllItems should contain(Some(InventoryItem(10, 1)))
  }

  it should " allow to move items" in {
    val dirtBlock = InventoryItem(10, 64)
    inventory.addItem(0, dirtBlock)
    inventory.moveItem(0, 1, 32)
    inventory.retrieveAllItems.head shouldBe (Some(InventoryItem(10, 32)))
    inventory.retrieveAllItems(1) shouldBe (Some(InventoryItem(10, 32)))
  }

  it should " allow to move items and stack them" in {
    val dirtBlock = InventoryItem(10, 32)
    inventory.addItem(0, dirtBlock)
    inventory.addItem(1, dirtBlock)
    inventory.moveItem(0, 1, 32)
    inventory.retrieveAllItems.head shouldBe (None)
    inventory.retrieveAllItems(1) shouldBe (Some(InventoryItem(10, 64)))
  }

  it should " allow to move items and stack them if possible accordingly to stack size" in {
    val dirtBlock = InventoryItem(10, 31)
    val dirtBlock1 = InventoryItem(10, 34) //stack size for dirt(10) is 64
    inventory.addItem(0, dirtBlock)
    inventory.addItem(1, dirtBlock1)
    inventory.moveItem(0, 1, 31)
    inventory.retrieveAllItems.head shouldBe (Some(InventoryItem(10, 1)))
    inventory.retrieveAllItems(1) shouldBe (Some(InventoryItem(10, 64)))
  }
}
