package io.scalacraft.logic.inventories.traits.actors

import io.scalacraft.logic.RecipeManager
import io.scalacraft.logic.commons.Message.ForwardToClient
import io.scalacraft.logic.commons.Traits.EnrichedActor
import io.scalacraft.logic.inventories.InventoryItem
import io.scalacraft.logic.inventories.traits.InventoryWithCrafting
import io.scalacraft.packets.DataTypes.Slot
import io.scalacraft.packets.clientbound.PlayPackets.ConfirmTransaction
import io.scalacraft.packets.serverbound.PlayPackets.ClickWindow

trait CraftingInventoryActor extends InventoryActor {
  this: EnrichedActor =>

  protected val inventory: InventoryWithCrafting
  protected val craftingOutputSlot: Int

  override def addItem(inventoryItem: InventoryItem): Unit = {
    super.addItem(inventoryItem)
    scanCraftingArea()
    updateClientInventory()
  }

  override def removeItem(slotIndex: Int, inventoryItem: InventoryItem): Unit = {
    super.removeItem(slotIndex, inventoryItem)
    scanCraftingArea()
    updateClientInventory()
  }

  override def clickWindow(click: ClickWindow, slot: Int, actionNumber: Int, clickedItem: Slot): Unit = {
    handleAction(click.actionPerformed(), slot, clickedItem, slot == craftingOutputSlot)
    player ! ForwardToClient(ConfirmTransaction(id, actionNumber, accepted = true))
    scanCraftingArea()
    updateClientInventory()
  }

  override def closeWindow(): Unit = {
    inventory.clearSlot(craftingOutputSlot)
    super.closeWindow()
  }

  private def scanCraftingArea(): Unit = {
    val craftingItems = inventory.retrieveCraftingItems
    inventory.clearCrafted()

    if (!craftingItems.forall(_.isDefined)) {
      RecipeManager.checkForRecipes(craftingItems) match {
        case Some(recipe) => inventory.addCrafted(InventoryItem(recipe.id, recipe.count))
        case None => inventory.clearCrafted()
      }
    }
  }

}
