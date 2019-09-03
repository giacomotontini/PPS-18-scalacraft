package io.scalacraft.logic.inventories.actors

import akka.actor.{ActorRef, Props}
import io.scalacraft.logic.commons.Message.{EquipmentChanged, ForwardToClient, PopulatePlayerInventory, RetrieveHeldItemId, UseHeldItem}
import io.scalacraft.logic.inventories.traits.actors.CraftingInventoryActor
import io.scalacraft.logic.inventories.{InventoryItem, PlayerInventory}
import io.scalacraft.packets.DataTypes.{Slot, SlotData}
import io.scalacraft.packets.serverbound.PlayPackets
import io.scalacraft.packets.serverbound.PlayPackets.HeldItemChange
import net.querz.nbt.CompoundTag

class PlayerInventoryActor(val player: ActorRef) extends CraftingInventoryActor {
  protected val inventory = PlayerInventory()
  protected val craftingOutputSlot: Int = PlayerInventory.CraftingOutputSlot
  protected val id: Int = PlayerInventory.Id
  private var heldSlot: Int = 0


  private def playerInventoryReceive: Receive = {
    case HeldItemChange(slot) =>
      heldSlot = slot
      heldItemChangeHandler
    case RetrieveHeldItemId =>
      sender ! inventory.findHeldItemId(heldSlot)
    case UseHeldItem =>
      sender ! inventory.useOneHeldItem(heldSlot)
      heldItemChangeHandler
    case PopulatePlayerInventory(inventoryItems: List[Option[InventoryItem]]) =>
      inventory.addPlayerInventory(inventoryItems)
      updateClientInventory
  }

  override def receive: Receive = playerInventoryReceive orElse defaultBehaviour

  override def addItem(inventoryItem: InventoryItem): Unit = {
    super.addItem(inventoryItem)
    heldItemChangeHandler
  }

  override def removeItem(slotIndex: Int, inventoryItem: InventoryItem): Unit = {
    super.removeItem(slotIndex, inventoryItem)
    if(inventory.mainHotInventoryRange.HotBarSlotRange.contains(slotIndex)) {
      heldItemChangeHandler
    }
  }

  override def clickWindow(click: PlayPackets.ClickWindow, slot: Int, actionNumber: Int, clickedItem: Slot): Unit = {
    super.clickWindow(click, slot, actionNumber, clickedItem)
    if(inventory.mainHotInventoryRange.HotBarSlotRange.contains(slot)) {
      heldItemChangeHandler
    }
  }

  private def heldItemChangeHandler(): Unit = {
      inventory.findHeldItem(heldSlot) match {
        case Some(heldItem) =>
          player ! ForwardToClient(EquipmentChanged(Some(SlotData(heldItem.itemId, heldItem.quantity, new CompoundTag()))))
        case None =>
          player ! ForwardToClient(EquipmentChanged(Some(SlotData(0, 0, new CompoundTag()))))
      }
  }

}

object PlayerInventoryActor {

  def props(userContext: ActorRef): Props = Props(new PlayerInventoryActor(userContext))

  def name(): String = s"PlayerInventory"
}