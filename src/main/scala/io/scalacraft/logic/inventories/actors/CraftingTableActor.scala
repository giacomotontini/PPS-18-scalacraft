package io.scalacraft.logic.inventories.actors

import akka.actor.{ActorRef, Props}
import io.scalacraft.logic.commons.Message._
import io.scalacraft.logic.inventories.CraftingTableInventory
import io.scalacraft.logic.inventories.traits.actors.CraftingInventoryActor

class CraftingTableActor(val id: Int, val player: ActorRef, val playerInventory: ActorRef) extends CraftingInventoryActor {
  protected val inventory = CraftingTableInventory(id)
  protected val craftingOutputSlot: Int = CraftingTableInventory.CraftingOutputSlot

  protected def craftingBehaviour: Receive = {
    case PopulatePlayerInventory(playerInventory) =>
      inventory.addPlayerInventory(playerInventory)
      updateClientInventory()
  }

  override def closeWindow(): Unit = {
    inventory.inventoryClosed()
    playerInventory ! PopulatePlayerInventory(inventory.retrieveInventoryItems())
  }

  override def receive: Receive = {
    defaultBehaviour orElse craftingBehaviour
  }
}

object CraftingTableActor {

  def props(id: Int, userContext: ActorRef, playerInventory: ActorRef): Props = Props(new CraftingTableActor(id, userContext, playerInventory))

  def name(id: Int): String = s"CraftingInventory-$id"
}