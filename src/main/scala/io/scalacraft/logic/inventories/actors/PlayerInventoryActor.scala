package io.scalacraft.logic.inventories.actors

import akka.actor.{ActorRef, Props}
import io.scalacraft.logic.inventories.traits.actors.CraftingInventoryActor
import io.scalacraft.logic.inventories.{InventoryItem, PlayerInventory}
import io.scalacraft.logic.messages.Message.{PopulatePlayerInventory, RetrieveHeldItemId, UseHeldItem}
import io.scalacraft.packets.serverbound.PlayPackets.HeldItemChange

class PlayerInventoryActor(val player: ActorRef) extends CraftingInventoryActor {
  protected val inventory = PlayerInventory()
  protected val craftingOutputSlot: Int = PlayerInventory.CraftingOutputSlot
  protected val id: Int = PlayerInventory.Id
  private var heldSlot: Int = 0


  private def playerInventoryReceive: Receive = {
    case HeldItemChange(slot) =>
      heldSlot = slot
    case RetrieveHeldItemId =>
      sender ! inventory.findHeldItemId(heldSlot)
    case UseHeldItem =>
      sender ! inventory.useOneHeldItem(heldSlot)
    case PopulatePlayerInventory(inventoryItems: List[Option[InventoryItem]]) =>
      inventory.addPlayerInventory(inventoryItems)
      updateClientInventory()
  }

  override def receive: Receive = playerInventoryReceive orElse defaultBehaviour

}

object PlayerInventoryActor {

  def props(userContext: ActorRef): Props = Props(new PlayerInventoryActor(userContext))

  def name(): String = s"PlayerInventory"
}