package io.scalacraft.logic.inventories

import io.scalacraft.logic.traits.inventories._
import io.scalacraft.logic.traits.inventories.range.{CraftingRange, MainHotRange}

case class PlayerInventory() extends InventoryWithPlayerInventory with InventoryWithCrafting {
  import PlayerInventory._

  override protected val inventory: Array[Option[InventoryItem]] = Array.fill(OffhandSlot + 1)(None: Option[InventoryItem])
  override protected val mainHotInventoryRange: MainHotRange = PlayerInventory
  override protected val craftingRange: CraftingRange = PlayerInventory
  override def id: Int = PlayerInventory.Id

  def findHeldItemId(hotSlot: Int): Option[Int] = {
    inventory(hotSlot + HotBarSlotRange.start).map(_.itemId)
  }

  def useOneHeldItem(hotSlot: Int): Option[Int] = {
    findHeldItemId(hotSlot) match {
      case id @ Some(itemId) =>
        removeItem(hotSlot + HotBarSlotRange.start, InventoryItem(itemId, 1))
        id
      case None => None
    }
  }

  override def retrieveInventoryItems(): List[Option[InventoryItem]] = {
    inventory.dropRight(1).takeRight(HotBarSlotRange.length + MainInventorySlotRange.length).toList
  }
}

object PlayerInventory extends MainHotRange with CraftingRange {
  def Id = 0  //PlayerInventory
  //All range boundaries are inclusive by protocol
  private[logic] def CraftingOutputSlot = 0
  private[logic] def CraftingInputSlotRange = Range(1, 4).inclusive
  private[logic] def ArmorSlotRange = Range(5, 8).inclusive
  private[logic] def MainInventorySlotRange = Range(9, 35).inclusive
  private[logic] def HotBarSlotRange = Range(36, 44).inclusive
  private[logic] def OffhandSlot = 45
}