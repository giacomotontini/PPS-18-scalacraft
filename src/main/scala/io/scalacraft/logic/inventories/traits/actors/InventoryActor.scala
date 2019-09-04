package io.scalacraft.logic.inventories.traits.actors

import akka.actor.ActorRef
import io.scalacraft.logic.commons.Message._
import io.scalacraft.logic.commons.Traits.EnrichedActor
import io.scalacraft.logic.inventories.InventoryItem
import io.scalacraft.logic.inventories.traits.{ClickWindowActionManager, Inventory}
import io.scalacraft.packets.DataTypes.{Slot, SlotData}
import io.scalacraft.packets.clientbound.PlayPackets.{ConfirmTransaction, SetSlot}
import io.scalacraft.packets.serverbound.PlayPackets.{ClickWindow, CloseWindow}
import net.querz.nbt.CompoundTag

trait InventoryActor extends ClickWindowActionManager {
  this: EnrichedActor =>

  protected val inventory: Inventory
  protected val player: ActorRef
  protected val id: Int

  def defaultBehaviour: Receive = {
    case LoadInventory => updateClientInventory()
    case AddItem(inventoryItem) => addItem(inventoryItem)
    case RemoveItem(slotIndex, inventoryItem) => removeItem(slotIndex, inventoryItem)
    case RetrieveAllItems => sender ! inventory.retrieveAllItems
    case RetrieveInventoryItems => sender ! inventory.retrieveInventoryItems
    case CloseWindow(_) => closeWindow()
    case click@ClickWindow(_, slot, _, actionNumber, _, clickedItem) =>
      clickWindow(click, slot, actionNumber, clickedItem)
  }

  protected def clickWindow(click: ClickWindow, slot: Int, actionNumber: Int, clickedItem: Slot): Unit = {
    handleAction(click.actionPerformed(), slot, clickedItem)
    player ! ForwardToClient(ConfirmTransaction(inventory.id, actionNumber, accepted = true))
    updateClientInventory()
  }

  protected def closeWindow(): Unit = {
    inventory.inventoryClosed()
    updateClientInventory()
  }

  protected def addItem(inventoryItem: InventoryItem): Unit = inventory.addItem(inventoryItem)

  protected def removeItem(slotIndex: Int, inventoryItem: InventoryItem): Unit =
    inventory.removeItem(slotIndex, inventoryItem)

  protected def updateClientInventory(): Unit = inventory.retrieveAllItems.zipWithIndex.collect {
    case (Some(item), slot) => SetSlot(id, slot, Some(SlotData(item.itemId, item.quantity, new CompoundTag())))
    case (None, slot) => SetSlot(id, slot, None)
  } foreach (player ! ForwardToClient(_))

}
