package io.scalacraft.logic

import akka.actor.{Actor, ActorRef}
import io.scalacraft.logic.PlayerInventoryActor.Message.{AddItem, MoveItem, RemoveItem, RetrieveAllItems}

class PlayerInventoryActor(playerActorRef: ActorRef) extends Actor {

  private val inventory: PlayerInventory = PlayerInventory()

  override def receive: Receive = {
    case AddItem(inventoryItem) =>
      inventory.addItem(inventoryItem)
    case RemoveItem(slotIndex, inventoryItem) =>
      inventory.removeItem(slotIndex, inventoryItem)
    case MoveItem(from, to, quantity) =>
      inventory.moveItem(from, to, quantity)
    case RetrieveAllItems => inventory.retrieveAllItems()
  }
}

object PlayerInventoryActor {
  sealed trait Message
  object Message {
    case class AddItem(inventoryItem: InventoryItem) extends Message
    case class RemoveItem(slotIndex: Int, inventoryItem: InventoryItem) extends Message
    case class MoveItem(fromSlot: Int, toSlot: Int, quantity: Int) extends Message
    case class RetrieveAllItems() extends Message
  }
}