package io.scalacraft.logic

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import io.scalacraft.logic.PlayerInventoryActor.Message._
import io.scalacraft.logic.messages.Message.ForwardToClient
import io.scalacraft.logic.traits.{ClickWindowActionManager, DefaultTimeout, ImplicitContext, RecipeManager}
import io.scalacraft.packets.DataTypes.SlotData
import io.scalacraft.packets.clientbound.PlayPackets.{ConfirmTransaction, SetSlot}
import io.scalacraft.packets.serverbound.PlayPackets.{ClickWindow, CloseWindow, HeldItemChange}
import net.querz.nbt.CompoundTag

class PlayerInventoryActor(playerActorRef: ActorRef) extends Actor with ActorLogging with DefaultTimeout with ImplicitContext with ClickWindowActionManager {
  override protected val inventory = PlayerInventory()
  private var heldSlot: Int = 0

  override def receive: Receive = {
    case AddItem(inventoryItem) =>
      inventory.addItem(inventoryItem)
      scanCraftingArea()
      updatePlayerInventory()
    case RemoveItem(slotIndex, inventoryItem) =>
      inventory.removeItem(slotIndex, inventoryItem)
      scanCraftingArea()
      updatePlayerInventory()
    case RetrieveAllItems =>
      sender ! inventory.retrieveAllItems()
    case HeldItemChange(slot) =>
      heldSlot = slot
    case RetrieveHeldItemId =>
      sender ! inventory.findHeldItemId(heldSlot)
    case UseHeldItem =>
      sender ! inventory.useOneHeldItem(heldSlot)
    case CloseWindow(_) =>
      inventory.inventoryClosed()
      updatePlayerInventory()
    case click@ClickWindow(_, slot, _, actionNumber, _, clickedItem) =>
      slot match {
        case _ if slot == PlayerInventory.CraftingOutputSlot =>
          handleActionOnCraftingSlot(click.actionPerformed(), slot, clickedItem)
        case _ =>
          handleAction(click.actionPerformed(), slot, clickedItem)
      }
      playerActorRef ! ForwardToClient(ConfirmTransaction(PlayerInventory.Id, actionNumber, accepted = true))
      scanCraftingArea()
      updatePlayerInventory()
  }

  private def updatePlayerInventory(): Unit = {
    inventory.retrieveAllItems().zipWithIndex.collect {
      case (Some(item), slot) =>
        val slotData = Some(SlotData(item.itemId, item.quantity, new CompoundTag()))
        SetSlot(PlayerInventory.Id, slot, slotData)
      case (None, slot) =>
        SetSlot(PlayerInventory.Id, slot, None)
    } foreach (playerActorRef ! ForwardToClient(_))
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

object PlayerInventoryActor {

  sealed trait Message

  object Message {

    case class AddItem(inventoryItem: InventoryItem) extends Message

    case class RemoveItem(slotIndex: Int, inventoryItem: InventoryItem) extends Message

    case class RetrieveAllItems() extends Message

    case class RetrieveHeldItemId() extends Message

    case class UseHeldItem() extends Message

  }

  def props(userContext: ActorRef): Props = Props(new PlayerInventoryActor(userContext))

  def name(): String = s"PlayerInventory"
}