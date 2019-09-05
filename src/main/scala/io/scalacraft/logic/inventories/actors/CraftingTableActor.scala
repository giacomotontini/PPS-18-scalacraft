package io.scalacraft.logic.inventories.actors

import akka.actor.{ActorRef, Props}
import io.scalacraft.logic.commons.Message._
import io.scalacraft.logic.commons.Traits.EnrichedActor
import io.scalacraft.logic.inventories.CraftingTableInventory
import io.scalacraft.logic.inventories.traits.actors.CraftingInventoryActor

/**
 * An actor which handle a session on a crafting table window.
 * @param id the identifier of this window (change every time a crafting table is opened)
 * @param player the player actor which opened the window
 * @param playerInventory the actor which hold the player inventory
 */
class CraftingTableActor(val id: Int, val player: ActorRef, val playerInventory: ActorRef) extends EnrichedActor
  with CraftingInventoryActor {

  protected val inventory = new CraftingTableInventory(id)
  protected val craftingOutputSlot: Int = CraftingTableInventory.CraftingOutputSlot

  protected def craftingBehaviour: Receive = {
    // when a crafting table is opened it should populate the player inventory section with a player inventory
    case PopulatePlayerInventory(playerInventory) =>
      inventory.addPlayerInventory(playerInventory)
      updateClientInventory()
  }

  /**
   * Close the current window and load back to the player inventory the player inventory section from the crafting table
   */
  override def closeWindow(): Unit = {
    inventory.inventoryClosed()
    playerInventory ! PopulatePlayerInventory(inventory.retrieveInventoryItems)
  }

  override def receive: Receive = defaultBehaviour orElse craftingBehaviour
}

object CraftingTableActor {

  def props(id: Int, userContext: ActorRef, playerInventory: ActorRef): Props =
    Props(new CraftingTableActor(id, userContext, playerInventory))

  def name(id: Int): String = s"CraftingInventory-$id"

}
