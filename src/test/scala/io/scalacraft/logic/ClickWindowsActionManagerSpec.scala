package io.scalacraft.logic

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.TestProbe
import io.scalacraft.logic.inventories.traits.Inventory
import io.scalacraft.logic.inventories.{InventoryItem, PlayerInventory}
import io.scalacraft.packets.DataTypes.SlotData
import io.scalacraft.packets.serverbound.PlayPackets.ClickWindowAction
import net.querz.nbt.CompoundTag
import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers}
import io.scalacraft.logic.inventories.traits.ClickWindowActionManager

class ClickWindowsActionManagerSpec extends FlatSpec with Matchers with BeforeAndAfterEach {

  implicit private val actorSystem = ActorSystem("test")

  case class DummyClickWindowsActionManager() extends ClickWindowActionManager {
    override val inventory: Inventory = new PlayerInventory
    override protected val player: ActorRef = TestProbe().testActor
  }

  var actionManager: DummyClickWindowsActionManager = _
  val dirt = InventoryItem(10, 31)
  val sand = InventoryItem(26, 32)
  val stick = InventoryItem(497, 4)
  val hotSlot: Int = PlayerInventory.HotBarSlotRange.start
  val outSlot: Int = -999
  val craftingSlot: Int = PlayerInventory.CraftingOutputSlot

  //All the following tests has dirt item loaded on hotBar's hotSlot
  override def beforeEach(): Unit = {
    actionManager = DummyClickWindowsActionManager()
    actionManager.inventory.addItem(hotSlot, dirt)
  }

  /* Hold dirt from slot 0 and put back on slot 1 */
  "A left click on a slot with item and a subsequent one on a free slot" should " move items from one place to another" in {
    actionManager.handleAction(ClickWindowAction.LeftMouseClick(), hotSlot, Some(SlotData(dirt.itemId, dirt.quantity, new CompoundTag())))
    actionManager.inventory.retrieveAllItems(hotSlot) shouldBe None

    actionManager.handleAction(ClickWindowAction.LeftMouseClick(), hotSlot + 1, None)
    actionManager.inventory.retrieveAllItems(hotSlot + 1) shouldBe Some(dirt)
  }

  /* Hold dirt from slot 0 and put back on slot 1 (check if quantities are stacked) */
  "A left click on a slot with item and subsequent one on a slot with same item type" should " stack items" in {
    actionManager.inventory.addItem(hotSlot + 1, dirt)

    actionManager.handleAction(ClickWindowAction.LeftMouseClick(), hotSlot, Some(SlotData(dirt.itemId, dirt.quantity, new CompoundTag())))
    actionManager.inventory.retrieveAllItems(hotSlot) shouldBe None

    actionManager.handleAction(ClickWindowAction.LeftMouseClick(), hotSlot + 1, Some(SlotData(dirt.itemId, dirt.quantity, new CompoundTag())))
    actionManager.inventory.retrieveAllItems(hotSlot + 1) shouldBe Some(InventoryItem(dirt.itemId, dirt.quantity * 2))
  }

  /* Hold dirt from slot 0, swap with sand on slot 1 and posing sand back to slot 2*/
  "A left click on a slot with item and subsequent one on a slot with different item type" should " swap hold items" in {
    actionManager.inventory.addItem(hotSlot + 1, sand)

    actionManager.handleAction(ClickWindowAction.LeftMouseClick(), hotSlot, Some(SlotData(dirt.itemId, dirt.quantity, new CompoundTag())))
    actionManager.inventory.retrieveAllItems(hotSlot) shouldBe None
    actionManager.inventory.retrieveAllItems(hotSlot + 1) shouldBe Some(sand)

    actionManager.handleAction(ClickWindowAction.LeftMouseClick(), hotSlot + 1, Some(SlotData(sand.itemId, sand.quantity, new CompoundTag())))
    actionManager.inventory.retrieveAllItems(hotSlot) shouldBe None
    actionManager.inventory.retrieveAllItems(hotSlot + 1) shouldBe Some(dirt)

    actionManager.handleAction(ClickWindowAction.LeftMouseClick(), hotSlot + 2, None)
    actionManager.inventory.retrieveAllItems(hotSlot) shouldBe None
    actionManager.inventory.retrieveAllItems(hotSlot + 1) shouldBe Some(dirt)
    actionManager.inventory.retrieveAllItems(hotSlot + 2) shouldBe Some(sand)
  }

  /* Hold sand from slot 1, put back on slot 2 (already full by stack size) and posing remaining items on a slot 3 slot to check correctness*/
  "A left click on a slot with item and subsequent one on a full slot" should " keep in hand items." in {
    actionManager.inventory.addItem(hotSlot + 1, sand)
    actionManager.inventory.addItem(hotSlot + 2, sand)
    actionManager.inventory.addItem(hotSlot + 2, sand)
    actionManager.inventory.retrieveAllItems(hotSlot) shouldBe Some(dirt)
    actionManager.inventory.retrieveAllItems(hotSlot + 1) shouldBe Some(InventoryItem(sand.itemId, sand.quantity))
    actionManager.inventory.retrieveAllItems(hotSlot + 2) shouldBe Some(InventoryItem(sand.itemId, sand.quantity * 2))
    actionManager.inventory.retrieveAllItems(hotSlot + 3) shouldBe None

    actionManager.handleAction(ClickWindowAction.LeftMouseClick(), hotSlot + 1, Some(SlotData(sand.itemId, sand.quantity, new CompoundTag())))
    actionManager.inventory.retrieveAllItems(hotSlot) shouldBe Some(dirt)
    actionManager.inventory.retrieveAllItems(hotSlot + 1) shouldBe None
    actionManager.inventory.retrieveAllItems(hotSlot + 2) shouldBe Some(InventoryItem(sand.itemId, sand.quantity * 2))
    actionManager.inventory.retrieveAllItems(hotSlot + 3) shouldBe None

    actionManager.handleAction(ClickWindowAction.LeftMouseClick(), hotSlot + 2, Some(SlotData(sand.itemId, sand.quantity * 2, new CompoundTag())))
    actionManager.inventory.retrieveAllItems(hotSlot) shouldBe Some(dirt)
    actionManager.inventory.retrieveAllItems(hotSlot + 1) shouldBe None
    actionManager.inventory.retrieveAllItems(hotSlot + 2) shouldBe Some(InventoryItem(sand.itemId, sand.quantity * 2))
    actionManager.inventory.retrieveAllItems(hotSlot + 3) shouldBe None

    actionManager.handleAction(ClickWindowAction.LeftMouseClick(), hotSlot + 3, None)
    actionManager.inventory.retrieveAllItems(hotSlot) shouldBe Some(dirt)
    actionManager.inventory.retrieveAllItems(hotSlot + 1) shouldBe None
    actionManager.inventory.retrieveAllItems(hotSlot + 2) shouldBe Some(InventoryItem(sand.itemId, sand.quantity * 2))
    actionManager.inventory.retrieveAllItems(hotSlot + 3) shouldBe Some(InventoryItem(sand.itemId, sand.quantity))
  }

  /* Hold the odd half of slot 0 on hand and posing back to slot 1*/
  "A right click on a slot with items" should "split them, leaving the even half on the slot and the remaining odd half hold " in {
    actionManager.handleAction(ClickWindowAction.RightMouseClick(), hotSlot, Some(SlotData(dirt.itemId, dirt.quantity, new CompoundTag())))
    actionManager.inventory.retrieveAllItems(hotSlot) shouldBe Some(InventoryItem(dirt.itemId, dirt.quantity / 2))

    actionManager.handleAction(ClickWindowAction.LeftMouseClick(), hotSlot + 1, None)
    actionManager.inventory.retrieveAllItems(hotSlot) shouldBe Some(InventoryItem(dirt.itemId, dirt.quantity / 2))
    actionManager.inventory.retrieveAllItems(hotSlot + 1) shouldBe Some(InventoryItem(dirt.itemId, (dirt.quantity + 1) / 2))
  }

  /* Hold the odd half of slot 0 on hand and posing back to slot 1 (empty)*/
  "A right click on a slot with items and posing one item with right click on a free slot" should " leave the correct number of items on a third slot with a left click" in {
    actionManager.handleAction(ClickWindowAction.RightMouseClick(), hotSlot, Some(SlotData(dirt.itemId, dirt.quantity, new CompoundTag())))
    actionManager.inventory.retrieveAllItems(hotSlot) shouldBe Some(InventoryItem(dirt.itemId, dirt.quantity / 2))

    actionManager.handleAction(ClickWindowAction.RightMouseClick(), hotSlot + 1, None)
    actionManager.inventory.retrieveAllItems(hotSlot) shouldBe Some(InventoryItem(dirt.itemId, dirt.quantity / 2))
    actionManager.inventory.retrieveAllItems(hotSlot + 1) shouldBe Some(InventoryItem(dirt.itemId, 1))

    actionManager.handleAction(ClickWindowAction.LeftMouseClick(), hotSlot + 2, None)
    actionManager.inventory.retrieveAllItems(hotSlot) shouldBe Some(InventoryItem(dirt.itemId, dirt.quantity / 2))
    actionManager.inventory.retrieveAllItems(hotSlot + 1) shouldBe Some(InventoryItem(dirt.itemId, 1))
    actionManager.inventory.retrieveAllItems(hotSlot + 2) shouldBe Some(InventoryItem(dirt.itemId, (dirt.quantity + 1) / 2 - 1))
  }

  /* Hold the odd half of slot 0 on hand and posing back to slot 1 (non empty). Checkyng grouping */
  "A right click on a slot with items and posing one item with right click on a slot with items" should " leave the correct number of items on a third slot with a left click" in {
    actionManager.inventory.addItem(hotSlot + 1, dirt)

    actionManager.handleAction(ClickWindowAction.RightMouseClick(), hotSlot, Some(SlotData(dirt.itemId, dirt.quantity, new CompoundTag())))
    actionManager.inventory.retrieveAllItems(hotSlot) shouldBe Some(InventoryItem(dirt.itemId, dirt.quantity / 2))

    actionManager.handleAction(ClickWindowAction.RightMouseClick(), hotSlot + 1, Some(SlotData(dirt.itemId, dirt.quantity, new CompoundTag())))
    actionManager.inventory.retrieveAllItems(hotSlot) shouldBe Some(InventoryItem(dirt.itemId, dirt.quantity / 2))
    actionManager.inventory.retrieveAllItems(hotSlot + 1) shouldBe Some(InventoryItem(dirt.itemId, dirt.quantity + 1))

    actionManager.handleAction(ClickWindowAction.LeftMouseClick(), hotSlot + 2, None)
    actionManager.inventory.retrieveAllItems(hotSlot) shouldBe Some(InventoryItem(dirt.itemId, dirt.quantity / 2))
    actionManager.inventory.retrieveAllItems(hotSlot + 1) shouldBe Some(InventoryItem(dirt.itemId, dirt.quantity + 1))
    actionManager.inventory.retrieveAllItems(hotSlot + 2) shouldBe Some(InventoryItem(dirt.itemId, (dirt.quantity + 1) / 2 - 1))
  }


  /* Hold the odd half of slot 0 on hand and swapped on slot 1 where sand is stored. Verifying the swap on slot 2 */
  "A right click on a slot with items and posing one item with right click on a slot with other items" should " swap hold items." in {
    actionManager.inventory.addItem(hotSlot + 1, sand)

    actionManager.handleAction(ClickWindowAction.RightMouseClick(), hotSlot, Some(SlotData(dirt.itemId, dirt.quantity, new CompoundTag())))
    actionManager.inventory.retrieveAllItems(hotSlot) shouldBe Some(InventoryItem(dirt.itemId, dirt.quantity / 2))
    actionManager.inventory.retrieveAllItems(hotSlot + 1) shouldBe Some(sand)

    actionManager.handleAction(ClickWindowAction.RightMouseClick(), hotSlot + 1, Some(SlotData(sand.itemId, sand.quantity, new CompoundTag())))
    actionManager.inventory.retrieveAllItems(hotSlot) shouldBe Some(InventoryItem(dirt.itemId, dirt.quantity / 2))
    actionManager.inventory.retrieveAllItems(hotSlot + 1) shouldBe Some(InventoryItem(dirt.itemId, (dirt.quantity + 1) / 2))
    actionManager.inventory.retrieveAllItems(hotSlot + 2) shouldBe None

    actionManager.handleAction(ClickWindowAction.LeftMouseClick(), hotSlot + 2, None)
    actionManager.inventory.retrieveAllItems(hotSlot) shouldBe Some(InventoryItem(dirt.itemId, dirt.quantity / 2))
    actionManager.inventory.retrieveAllItems(hotSlot + 1) shouldBe Some(InventoryItem(dirt.itemId, (dirt.quantity + 1) / 2))
    actionManager.inventory.retrieveAllItems(hotSlot + 2) shouldBe Some(sand)
  }

  /* Hold the odd half of slot 0 on hand and swapped on slot 1 where sand is stored. Verifying the swap on slot 2 */
  "A right click on a slot with items and posing one item with left click on a slot with other items" should " swap hold items." in {
    actionManager.inventory.addItem(hotSlot + 1, sand)

    actionManager.handleAction(ClickWindowAction.RightMouseClick(), hotSlot, Some(SlotData(dirt.itemId, dirt.quantity, new CompoundTag())))
    actionManager.inventory.retrieveAllItems(hotSlot) shouldBe Some(InventoryItem(dirt.itemId, dirt.quantity / 2))
    actionManager.inventory.retrieveAllItems(hotSlot + 1) shouldBe Some(sand)

    actionManager.handleAction(ClickWindowAction.LeftMouseClick(), hotSlot + 1, Some(SlotData(sand.itemId, sand.quantity, new CompoundTag())))
    actionManager.inventory.retrieveAllItems(hotSlot) shouldBe Some(InventoryItem(dirt.itemId, dirt.quantity / 2))
    actionManager.inventory.retrieveAllItems(hotSlot + 1) shouldBe Some(InventoryItem(dirt.itemId, (dirt.quantity + 1) / 2))
    actionManager.inventory.retrieveAllItems(hotSlot + 2) shouldBe None

    actionManager.handleAction(ClickWindowAction.LeftMouseClick(), hotSlot + 2, None)
    actionManager.inventory.retrieveAllItems(hotSlot) shouldBe Some(InventoryItem(dirt.itemId, dirt.quantity / 2))
    actionManager.inventory.retrieveAllItems(hotSlot + 1) shouldBe Some(InventoryItem(dirt.itemId, (dirt.quantity + 1) / 2))
    actionManager.inventory.retrieveAllItems(hotSlot + 2) shouldBe Some(sand)
  }

  /***** Slot -999 tests *****/

  "A left click outside inventory with hold items" should " drop all the items" in {

    actionManager.handleAction(ClickWindowAction.RightMouseClick(), hotSlot, Some(SlotData(dirt.itemId, dirt.quantity, new CompoundTag())))
    actionManager.inventory.retrieveAllItems(hotSlot) shouldBe Some(InventoryItem(dirt.itemId, dirt.quantity / 2))

    actionManager.handleAction(ClickWindowAction.LeftMouseClick(), outSlot, None)
    actionManager.handleAction(ClickWindowAction.LeftMouseClick(), hotSlot + 1, None)
    actionManager.inventory.retrieveAllItems(hotSlot) shouldBe Some(InventoryItem(dirt.itemId, dirt.quantity / 2))
    actionManager.inventory.retrieveAllItems(hotSlot + 1) shouldBe None
  }

  "A right click outside inventory with hold items" should " drop one items, and hold all the other" in {
    actionManager.inventory.addItem(hotSlot, dirt)

    actionManager.handleAction(ClickWindowAction.RightMouseClick(), hotSlot, Some(SlotData(dirt.itemId, dirt.quantity * 2, new CompoundTag())))
    actionManager.inventory.retrieveAllItems(hotSlot) shouldBe Some(InventoryItem(dirt.itemId, dirt.quantity))

    actionManager.handleAction(ClickWindowAction.RightMouseClick(), outSlot, None)
    actionManager.handleAction(ClickWindowAction.LeftMouseClick(), hotSlot + 1, None)
    actionManager.inventory.retrieveAllItems(hotSlot) shouldBe Some(InventoryItem(dirt.itemId, dirt.quantity))
    actionManager.inventory.retrieveAllItems(hotSlot + 1) shouldBe Some(InventoryItem(dirt.itemId, dirt.quantity - 1))
  }

  /***** Crafting slot test *****/
  "A click on crafting slot with nothing in hand" should "grab the crafted item" in {
    actionManager.inventory.addItem(craftingSlot, stick)

    actionManager.handleAction(ClickWindowAction.LeftMouseClick(), craftingSlot, Some(SlotData(stick.itemId, stick.quantity, new CompoundTag())), true)
    actionManager.inventory.retrieveAllItems(craftingSlot) shouldBe None

    actionManager.handleAction(ClickWindowAction.LeftMouseClick(), hotSlot + 1, None)
    actionManager.inventory.retrieveAllItems(hotSlot + 1) shouldBe Some(InventoryItem(stick.itemId, stick.quantity))
  }


  "A click on crafting slot with same items on hand" should "grab the crafted item and group them" in {
    actionManager.inventory.addItem(craftingSlot, stick)
    actionManager.inventory.addItem(hotSlot + 1, stick)

    actionManager.handleAction(ClickWindowAction.LeftMouseClick(), hotSlot + 1, Some(SlotData(stick.itemId, stick.quantity, new CompoundTag())))
    actionManager.inventory.retrieveAllItems(hotSlot + 1) shouldBe None

    actionManager.handleAction(ClickWindowAction.LeftMouseClick(), craftingSlot, Some(SlotData(stick.itemId, stick.quantity, new CompoundTag())), true)
    actionManager.inventory.retrieveAllItems(craftingSlot) shouldBe None

    actionManager.handleAction(ClickWindowAction.LeftMouseClick(), hotSlot + 1, None)
    actionManager.inventory.retrieveAllItems(hotSlot + 1) shouldBe Some(InventoryItem(stick.itemId, stick.quantity * 2))
  }


}
