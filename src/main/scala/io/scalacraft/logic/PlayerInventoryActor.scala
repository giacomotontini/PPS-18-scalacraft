package io.scalacraft.logic

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import io.scalacraft.logic.PlayerInventoryActor.Message._
import io.scalacraft.logic.messages.Message.ForwardToClient
import io.scalacraft.logic.traits.{ClickWindowActionManager, DefaultTimeout, ImplicitContext}
import io.scalacraft.packets.DataTypes.SlotData
import io.scalacraft.packets.clientbound.PlayPackets.{ConfirmTransaction, SetSlot}
import io.scalacraft.packets.serverbound.PlayPackets.{ClickWindow, HeldItemChange}
import net.querz.nbt.CompoundTag

class PlayerInventoryActor(playerActorRef: ActorRef) extends Actor with ActorLogging with DefaultTimeout with ImplicitContext with ClickWindowActionManager {

  override protected val inventory = PlayerInventory()
  private var heldSlot: Int = 0

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
      heldSlot = slot
    case RetrieveHeldItemId =>
      sender ! inventory.findHeldItemId(heldSlot)
    case UseHeldItem =>
      sender ! inventory.useOneHeldItem(heldSlot)
    case click@ClickWindow(_, slot, _, actionNumber, _, clickedItem) =>
      handleAction(click.actionPerformed(), slot, clickedItem)
      playerActorRef ! ForwardToClient(ConfirmTransaction(PlayerInventory.Id, actionNumber, accepted = true))
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

    case class RetrieveHeldItemId() extends Message

    case class UseHeldItem() extends Message

  }

  def props(userContext: ActorRef): Props = Props(new PlayerInventoryActor(userContext))

  def name(): String = s"PlayerInventory"
}