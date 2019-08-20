package io.scalacraft.logic.traits

import akka.actor.{Actor, ActorLogging, ActorRef}
import io.scalacraft.logic.messages.Message.{AddItem, ForwardToClient, RemoveItem, RetrieveAllItems}
import io.scalacraft.logic.{Inventory, InventoryItem}
import io.scalacraft.packets.DataTypes.{Slot, SlotData}
import io.scalacraft.packets.clientbound.PlayPackets.{ConfirmTransaction, SetSlot}
import io.scalacraft.packets.serverbound.PlayPackets.{ClickWindow, CloseWindow}
import net.querz.nbt.CompoundTag

trait InventoryActor extends Actor with ActorLogging with DefaultTimeout with ImplicitContext with ClickWindowActionManager {
  protected val inventory: Inventory
  protected val playerActorRef: ActorRef
  protected val id: Int

  def defaultBehaviour: Receive = {
    case AddItem(inventoryItem) =>
      addItem(inventoryItem)
    case RemoveItem(slotIndex, inventoryItem) =>
      removeItem(slotIndex, inventoryItem)
    case RetrieveAllItems =>
      sender ! inventory.retrieveAllItems()
    case CloseWindow(_) =>
      closeWindow()
    case click@ClickWindow(_, slot, _, actionNumber, _, clickedItem) =>
      clickWindow(click, slot, actionNumber, clickedItem)
  }

  protected def clickWindow(click: ClickWindow, slot: Int, actionNumber: Int, clickedItem: Slot): Unit = {
    handleAction(click.actionPerformed(), slot, clickedItem)
    playerActorRef ! ForwardToClient(ConfirmTransaction(inventory.id, actionNumber, accepted = true))
    updateClientInventory()
  }

  protected def closeWindow(): Unit = {
    inventory.inventoryClosed()
    updateClientInventory()
  }

  protected def addItem(inventoryItem: InventoryItem): Unit = {
    inventory.addItem(inventoryItem)
  }

  protected def removeItem(slotIndex: Int, inventoryItem: InventoryItem): Unit = {
    inventory.removeItem(slotIndex, inventoryItem)
  }

  protected def updateClientInventory(): Unit = {
    inventory.retrieveAllItems().zipWithIndex.collect {
      case (Some(item), slot) =>
        val slotData = Some(SlotData(item.itemId, item.quantity, new CompoundTag()))
        SetSlot(id, slot, slotData)
      case (None, slot) =>
        SetSlot(id, slot, None)
    } foreach (playerActorRef ! ForwardToClient(_))
  }
}
