package io.scalacraft.logic

import akka.actor.{ActorRef, Props}
import io.scalacraft.logic.messages.Message._
import io.scalacraft.logic.traits.{CraftingInventoryActor}

class CraftingTableActor(val id: Int, val playerActorRef: ActorRef, val playerInventoryActorRef: ActorRef) extends CraftingInventoryActor {
  protected val inventory = CraftingTableInventory(id)
  protected val craftingOutputSlot: Int = CraftingTableInventory.CraftingOutputSlot

  protected def craftingBehaviour: Receive = {
    case PopulatePlayerInventory(playerInventory) =>
      inventory.addPlayerInventory(playerInventory)
      updateClientInventory()
  }

  override def closeWindow(): Unit = {
    inventory.inventoryClosed()
    playerInventoryActorRef ! PopulatePlayerInventory(inventory.retrieveInventoryItems())
  }

  override def receive: Receive = {
    defaultBehaviour orElse craftingBehaviour
  }
}

object CraftingTableActor {

  def props(id: Int, userContext: ActorRef, playerInventoryActorRef: ActorRef): Props = Props(new CraftingTableActor(id, userContext, playerInventoryActorRef))

  def name(id: Int): String = s"CraftingInventory-$id"
}