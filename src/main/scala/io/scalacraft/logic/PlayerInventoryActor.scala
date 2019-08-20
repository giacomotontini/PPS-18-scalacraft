package io.scalacraft.logic

import akka.actor.{ActorRef, Props}
import io.scalacraft.logic.messages.Message.{RetrieveHeldItemId, UseHeldItem}
import io.scalacraft.packets.serverbound.PlayPackets.HeldItemChange

class PlayerInventoryActor(override protected val playerActorRef: ActorRef) extends CraftingInventoryActor(PlayerInventory.Id, playerActorRef) {
  override protected val inventory: PlayerInventory = PlayerInventory()
  override protected val craftingOutputSlot: Int = PlayerInventory.CraftingOutputSlot
  private var heldSlot: Int = 0


  private def playerInventoryReceive: Receive = {
    case HeldItemChange(slot) =>
      heldSlot = slot
    case RetrieveHeldItemId =>
      sender ! inventory.findHeldItemId(heldSlot)
    case UseHeldItem =>
      sender ! inventory.useOneHeldItem(heldSlot)
  }

  override def receive: Receive = playerInventoryReceive orElse defaultBehaviour

}

object PlayerInventoryActor {

  def props(userContext: ActorRef): Props = Props(new PlayerInventoryActor(userContext))

  def name(): String = s"PlayerInventory"
}