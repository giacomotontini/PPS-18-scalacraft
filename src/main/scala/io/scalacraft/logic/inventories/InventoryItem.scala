package io.scalacraft.logic.inventories

case class InventoryItem(itemId: Int, var quantity: Int = 0) {

  def +(inventoryItem: InventoryItem): InventoryItem = {
    require(this.itemId == inventoryItem.itemId, "Sum of different inventory item not allowed.")
    InventoryItem(itemId, quantity + inventoryItem.quantity)
  }

}
