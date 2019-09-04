package io.scalacraft.logic.inventories

import io.scalacraft.logic.inventories.traits.range.{CraftingRange, MainHotRange}
import io.scalacraft.logic.inventories.traits.{InventoryWithCrafting, InventoryWithPlayerInventory}

class PlayerInventory extends InventoryWithPlayerInventory with InventoryWithCrafting {

  import PlayerInventory._

  override protected val inventory: Array[Option[InventoryItem]] = Array.fill(OffhandSlot + 1)(None)
  override protected[inventories] val mainHotInventoryRange: MainHotRange = PlayerInventory
  override protected[inventories] val craftingRange: CraftingRange = PlayerInventory
  override val id: Int = PlayerInventory.Id

  def findHeldItemId(hotSlot: Int): Option[Int] = findHeldItem(hotSlot).map(_.itemId)

  def findHeldItem(hotSlot: Int): Option[InventoryItem] = inventory(hotSlot + HotBarSlotRange.start)

  def useOneHeldItem(hotSlot: Int): Option[Int] = findHeldItemId(hotSlot) match {
    case id@Some(itemId) =>
      removeItem(hotSlot + HotBarSlotRange.start, InventoryItem(itemId, 1))
      id
    case None => None
  }
}

object PlayerInventory extends MainHotRange with CraftingRange {

  val Id = 0 //PlayerInventory
  //All range boundaries are inclusive by protocol
  private[logic] val CraftingOutputSlot = 0
  private[logic] val CraftingInputSlotRange = 1 to 4
  private[logic] val ArmorSlotRange = 5 to 8
  private[logic] val MainInventorySlotRange = 9 to 35
  private[logic] val HotBarSlotRange = 36 to 44
  private[logic] val OffhandSlot = 45

}
