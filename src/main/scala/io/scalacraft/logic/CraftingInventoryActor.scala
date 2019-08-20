package io.scalacraft.logic

import akka.actor.{ActorRef, Props}
import io.scalacraft.logic.messages.Message.{ForwardToClient, _}
import io.scalacraft.logic.traits.InventoryActor
import io.scalacraft.packets.DataTypes.Slot
import io.scalacraft.packets.clientbound.PlayPackets.ConfirmTransaction
import io.scalacraft.packets.serverbound.PlayPackets.ClickWindow

class CraftingInventoryActor(val id: Int, protected val playerActorRef: ActorRef) extends InventoryActor {
  override protected val inventory: InventoryWithCrafting = CraftingTableInventory(id)
  protected val craftingOutputSlot: Int = CraftingTableInventory.CraftingOutputSlot

  override def receive: Receive = defaultBehaviour

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
    playerActorRef ! ForwardToClient(ConfirmTransaction(id, actionNumber, accepted = true))
    scanCraftingArea()
    updateClientInventory()
  }

  private def scanCraftingArea(): Unit = {
    val craftingItems = inventory.retrieveCraftingItems()
    inventory.clearCrafted()
    if (!craftingItems.forall(_.isEmpty)) {
      RecipeManager.checkForRecipes(craftingItems) match {
        case Some(recipe) => inventory.addCrafted(InventoryItem(recipe.id, recipe.count))
        case None => inventory.clearCrafted()
      }
    }
  }
}

object CraftingInventoryActor {

  def props(id: Int, userContext: ActorRef): Props = Props(new CraftingInventoryActor(id, userContext))

  def name(id: Int): String = s"CraftingInventory-$id"
}