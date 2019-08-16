package io.scalacraft.logic

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import io.scalacraft.logic.PlayerInventoryActor.Message.{AddItem, MoveItem, RemoveItem, RetrieveAllItems, RetrieveHeldedItemId, UseHeldedItem}
import io.scalacraft.logic.messages.Message.ForwardToClient
import io.scalacraft.logic.traits.{DefaultTimeout, ImplicitContext}
import io.scalacraft.packets.DataTypes.{Slot, SlotData}
import io.scalacraft.packets.clientbound.PlayPackets.{ConfirmTransaction, SetSlot, Tag}
import io.scalacraft.packets.serverbound.PlayPackets.ClickWindowAction.{LeftMouseClick, LeftMouseDrag, RightMouseClick, RightMouseDrag}
import io.scalacraft.packets.serverbound.PlayPackets.{ClickWindow, HeldItemChange}
import net.querz.nbt.CompoundTag

class PlayerInventoryActor(playerActorRef: ActorRef) extends Actor with ActorLogging with DefaultTimeout with ImplicitContext {

  private val inventory: PlayerInventory = PlayerInventory()
  private var heldedSlot: Int = 0
  private var movingItems: Slot = None

  override def receive: Receive = {
    case AddItem(inventoryItem) =>
      inventory.addItem(inventoryItem)
      updatePlayerInventory()
    case RemoveItem(slotIndex, inventoryItem) =>
      inventory.removeItem(slotIndex, inventoryItem)
      updatePlayerInventory()
    case MoveItem(from, to, quantity) =>
      inventory.moveItem(from, to, quantity)
      updatePlayerInventory()
    case RetrieveAllItems =>
      updatePlayerInventory()
    case HeldItemChange(slot) =>
      heldedSlot = slot
    case RetrieveHeldedItemId =>
      sender ! inventory.findHeldedItemId(heldedSlot)
    case UseHeldedItem =>
      sender ! inventory.removeUsedHeldedItem(heldedSlot)
    case click@ClickWindow(_, slot, _, actionNumber, _, clickedItem) =>
      click.actionPerformed() match {
        case LeftMouseClick(_) | LeftMouseDrag(true, false)=>
          if (clickedItem.isDefined) {
            if (movingItems.isDefined) {
              if (clickedItem.get.itemId == movingItems.get.itemId) { //grouping items
                val left = inventory.addItem(slot, InventoryItem(movingItems.get.itemId, movingItems.get.itemCount))
                if (left > 0) {
                  log.debug("Grouping items with items left.")
                  movingItems.get.itemCount = left
                } else {
                  log.debug("Grouping items withOUT items left.")
                  movingItems = None
                }
              } else { //swap holded items
                log.debug("Swap holded items")
                inventory.removeItem(slot, InventoryItem(clickedItem.get.itemId, clickedItem.get.itemCount))
                inventory.addItem(slot, InventoryItem(movingItems.get.itemId, movingItems.get.itemCount))
                movingItems = clickedItem
              }
            } else { //holding new items
              log.debug("Hold new items")
              inventory.removeItem(slot, InventoryItem(clickedItem.get.itemId, clickedItem.get.itemCount))
              movingItems = clickedItem
            }
          } else if (movingItems.isDefined) { //release new items
            log.debug("Release new items")
            inventory.addItem(slot, InventoryItem(movingItems.get.itemId, movingItems.get.itemCount))
            movingItems = None
          }
        case RightMouseClick(_) | RightMouseDrag(true, false) =>
          if (clickedItem.isDefined) {
            if (movingItems.isDefined) {
              if (clickedItem.get.itemId == movingItems.get.itemId) { // releasing one by one
                inventory.addItem(slot, InventoryItem(movingItems.get.itemId, 1))
                val left = movingItems.get.itemCount - 1
                if (left > 0) {
                  log.debug("Release one by one on same itemId with some items left.")
                  movingItems.get.itemCount = left
                } else {
                  log.debug("Release one by one on same itemId withOUT items left.")
                  movingItems = None
                }
              } else { // swap holded items
                log.debug("Swap holded items.")
                inventory.removeItem(slot, InventoryItem(clickedItem.get.itemId, clickedItem.get.itemCount))
                inventory.addItem(slot, InventoryItem(movingItems.get.itemId, movingItems.get.itemCount))
                movingItems = clickedItem
              }
            } else { //splitting
              log.debug("Splitting.")
              val movingQuantityLeft: Int = (clickedItem.get.itemCount + 1) / 2
              inventory.removeItem(slot, InventoryItem(clickedItem.get.itemId, movingQuantityLeft))
              clickedItem.get.itemCount = movingQuantityLeft
              movingItems = clickedItem
            }
          } else if (movingItems.isDefined) { //release one by one
            log.debug("Release one by one on empty slot.")
            inventory.addItem(slot, InventoryItem(movingItems.get.itemId, 1))
            movingItems.get.itemCount -= 1
            if (movingItems.get.itemCount == 0) {
              movingItems = None
            }
          }

        case _ => //ignored
      }

      playerActorRef ! ForwardToClient(ConfirmTransaction(PlayerInventory.Id, actionNumber, true))
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
}

object PlayerInventoryActor {

  sealed trait Message

  object Message {

    case class AddItem(inventoryItem: InventoryItem) extends Message

    case class RemoveItem(slotIndex: Int, inventoryItem: InventoryItem) extends Message

    case class MoveItem(fromSlot: Int, toSlot: Int, quantity: Int) extends Message

    case class RetrieveAllItems() extends Message

    case class RetrieveHeldedItemId() extends Message

    case class UseHeldedItem() extends Message

  }

  def props(userContext: ActorRef): Props = Props(new PlayerInventoryActor(userContext))

  def name(): String = s"PlayerInventory"
}