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

/**
 * The base actor for each kind of inventory. All the basic operation on an inventory are handled here,
 * including those made with mouse by the user.
 */
trait InventoryActor extends ClickWindowActionManager {
  this: EnrichedActor =>

  protected val inventory: Inventory
  protected val player: ActorRef // the player actor ref to which this inventory is binded to
  protected val id: Int // the inventory id. Unique for all the window's inventories.

  def defaultBehaviour: Receive = {
    // the initialization message for showing up the inventory upon login
    case LoadInventory => updateClientInventory()
    case AddItem(inventoryItem) => addItem(inventoryItem)
    case RemoveItem(slotIndex, inventoryItem) => removeItem(slotIndex, inventoryItem)
    case RetrieveAllItems => sender ! inventory.retrieveAllItems
    case RetrieveInventoryItems => sender ! inventory.retrieveInventoryItems
    case CloseWindow(_) => closeWindow()
    case click@ClickWindow(_, slot, _, actionNumber, _, clickedItem) =>
      clickWindow(click, slot, actionNumber, clickedItem)
  }

  /**
   *
   * @param click
   * @param slot on which user clicked
   * @param actionNumber the number of the action performed. @see <a href="https://wiki.vg/Protocol#Click_Window">ClickWindow</a>
   * @param clickedItem the items on slot subject to this action
   */
  protected def clickWindow(click: ClickWindow, slot: Int, actionNumber: Int, clickedItem: Slot): Unit = {
    handleAction(click.actionPerformed(), slot, clickedItem)
    player ! ForwardToClient(ConfirmTransaction(inventory.id, actionNumber, accepted = true))
    updateClientInventory()
  }

  /**
   * Close this inventory and for each specific implementation do specific things.
   * Client side is updated.
   */
  protected def closeWindow(): Unit = {
    inventory.inventoryClosed()
    updateClientInventory()
  }

  /**
   * Add an item to this inventory. A slot isn't required, the first compatible one will be used.
   * @param inventoryItem the item type that need to be added (with quantity)
   */
  protected def addItem(inventoryItem: InventoryItem): Unit = inventory.addItem(inventoryItem)

  /**
   * Remove an item from this inventory. A specific slot is required.
   * @param slotIndex the slot from which the item should be removed.
   * @param inventoryItem the item type that need to be removed (with quantity)
   */
  protected def removeItem(slotIndex: Int, inventoryItem: InventoryItem): Unit =
    inventory.removeItem(slotIndex, inventoryItem)

  /**
   * Update the client side windows which show this inventory. Every slot is sent, even if the previous
   * involved operation didn't affect all slots.
   */
  protected def updateClientInventory(): Unit = inventory.retrieveAllItems.zipWithIndex.collect {
    case (Some(item), slot) => SetSlot(id, slot, Some(SlotData(item.itemId, item.quantity, new CompoundTag())))
    case (None, slot) => SetSlot(id, slot, None)
  } foreach (player ! ForwardToClient(_))

}
