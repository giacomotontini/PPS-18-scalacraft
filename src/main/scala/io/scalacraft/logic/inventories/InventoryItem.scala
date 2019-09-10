package io.scalacraft.logic.inventories

/**
 * The base element kept on inventories.
 * @param itemId the type of the item
 * @param quantity the number of items
 */
case class InventoryItem(itemId: Int, var quantity: Int = 0) {

  def +(inventoryItem: InventoryItem): InventoryItem = {
    require(this.itemId == inventoryItem.itemId, "Sum of different inventory item not allowed.")
    InventoryItem(itemId, quantity + inventoryItem.quantity)
  }

}
