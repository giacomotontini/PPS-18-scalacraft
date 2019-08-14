package io.scalacraft.logic

import akka.actor.{Actor, ActorRef, Props}
import io.scalacraft.logic.PlayerInventoryActor.Message.{AddItem, MoveItem, RemoveItem, RetrieveAllItems, RetrieveHeldedItemId, UseHeldedItem}
import io.scalacraft.logic.messages.Message.ForwardToClient
import io.scalacraft.packets.DataTypes.{Slot, SlotData}
import io.scalacraft.packets.clientbound.PlayPackets.SetSlot
import io.scalacraft.packets.serverbound.PlayPackets.ClickWindowAction.{LeftMouseClick, RightMouseClick}
import io.scalacraft.packets.serverbound.PlayPackets.{ClickWindow, HeldItemChange}
import net.querz.nbt.CompoundTag

class PlayerInventoryActor(playerActorRef: ActorRef) extends Actor {

  private val inventory: PlayerInventory = PlayerInventory()
  private var heldedSlot: Int = 0
  private var itemsSubjectToAction: Slot = None

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
    case click @ ClickWindow(_, slot, _, actionNumber, _, clickedItem) =>
      print(click)
      click.actionPerformed() match {
        case LeftMouseClick(_) =>
          if(clickedItem.isDefined) { // moving items start
            itemsSubjectToAction = clickedItem
            val item = clickedItem.get
            self ! RemoveItem(slot, InventoryItem(item.itemId, item.itemCount))
          } else {
            val item = itemsSubjectToAction.get
            itemsSubjectToAction = None
            self ! AddItem(InventoryItem(item.itemId, item.itemCount))
          }
        case RightMouseClick(_) => println("ciao")
      }
  }

  private def updatePlayerInventory(): Unit = {
    inventory.retrieveAllItems().zipWithIndex.collect {
      case (Some(item), slot) =>
        val slotData = Some(SlotData(item.itemId, item.quantity, new CompoundTag()))
        SetSlot(PlayerInventory.Id, slot, slotData)
      case (None, slot) =>
        SetSlot(PlayerInventory.Id, slot, None)
    } foreach(playerActorRef ! ForwardToClient(_))
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