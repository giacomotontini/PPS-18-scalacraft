package io.scalacraft.logic.inventories.actors

import akka.actor.{ActorRef, Props}
import io.scalacraft.logic.commons.Message._
import io.scalacraft.logic.commons.Traits.EnrichedActor
import io.scalacraft.logic.inventories.traits.actors.CraftingInventoryActor
import io.scalacraft.logic.inventories.{InventoryItem, PlayerInventory}
import io.scalacraft.core.packets.DataTypes.{Slot, SlotData}
import io.scalacraft.core.packets.serverbound.PlayPackets
import io.scalacraft.core.packets.serverbound.PlayPackets.HeldItemChange
import net.querz.nbt.CompoundTag

/**
 * Represent an actor which take care of operation made on the inventory of a single player. Every player must have one.
 *
 * @param player the player to which this inventory correspond to
 */
class PlayerInventoryActor(val player: ActorRef) extends EnrichedActor with CraftingInventoryActor {

  protected val inventory = new PlayerInventory
  protected val craftingOutputSlot: Int = PlayerInventory.CraftingOutputSlot
  protected val id: Int = PlayerInventory.Id //all the player inventory window has the same id, by protocol.
  private var heldSlot: Int = 0 //the slot which is actually held by the player. Goes from 0 to 9 (It's the HotBar)

  private def playerInventoryReceive: Receive = {
    /* When a player change the selected slot we must record it for further use an in case of
     a non empty slot we must inform al the other player of the new held item */
    case HeldItemChange(slot) =>
      heldSlot = slot
      heldItemChangeHandler()
    /* Needed when user hit something. We must know what is actually holding in hand*/
    case RetrieveHeldItemId =>
      sender ! inventory.findHeldItemId(heldSlot)
    /* Needed when user place a block or (in future development) consume items lifetime*/
    case UseHeldItem =>
      sender ! inventory.useOneHeldItem(heldSlot)
      heldItemChangeHandler()
    /* The player inventory is loaded within other inventories. When they are closed,
    this message is used to update the source player inventory */
    case PopulatePlayerInventory(inventoryItems: List[Option[InventoryItem]]) =>
      inventory.addPlayerInventory(inventoryItems)
      updateClientInventory()
  }

  override def receive: Receive = playerInventoryReceive orElse defaultBehaviour

  override def addItem(inventoryItem: InventoryItem): Unit = {
    super.addItem(inventoryItem)
    heldItemChangeHandler()
  }

  override def removeItem(slotIndex: Int, inventoryItem: InventoryItem): Unit = {
    super.removeItem(slotIndex, inventoryItem)
    if (inventory.mainHotInventoryRange.HotBarSlotRange.contains(slotIndex)) {
      heldItemChangeHandler()
    }
  }

  override def clickWindow(click: PlayPackets.ClickWindow, slot: Int, actionNumber: Int, clickedItem: Slot): Unit = {
    super.clickWindow(click, slot, actionNumber, clickedItem)
    if (inventory.mainHotInventoryRange.HotBarSlotRange.contains(slot)) {
      heldItemChangeHandler()
    }
  }

  /**
   * Update other player of the current held item (equipment), even when nothing is held.
   */
  private def heldItemChangeHandler(): Unit = inventory.findHeldItem(heldSlot) match {
    case Some(heldItem) =>
      player ! ForwardToClient(EquipmentChanged(Some(SlotData(heldItem.itemId, heldItem.quantity, new CompoundTag()))))
    case None =>
      player ! ForwardToClient(EquipmentChanged(Some(SlotData(0, 0, new CompoundTag()))))
  }

}

object PlayerInventoryActor {

  def props(userContext: ActorRef): Props = Props(new PlayerInventoryActor(userContext))

  def name(): String = s"PlayerInventory"

}