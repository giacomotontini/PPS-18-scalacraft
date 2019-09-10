package io.scalacraft.loaders

import io.circe.generic.auto._
import io.circe.parser

import scala.io.Source

/**
 * A data loader for items.
 */
object Items {

  /**
   * Represent a minecraft item
   * @param id the item id
   * @param displayName the (pretty) item name
   * @param name the item name
   * @param stackSize max item that can be stacked on same inventory slot
   * @param attackSpeed the speed with whom this item attack
   * @param attackDamage the damage which this item cause
   */
  case class StorableItem(id: Int,
                          displayName: String,
                          name: String,
                          stackSize: Int,
                          attackSpeed: Float,
                          attackDamage: Float)

  /*
  Contains all items of the game
  */
  private lazy val storableItems = {
    val content = Source.fromInputStream(getClass.getResourceAsStream("/data/items.json")).mkString
    val Right(items) = parser.decode[List[StorableItem]](content)
    items
  }

  /**
   * Retrieve an item from a given item id
   * @param itemId the item id relative to the item to look for
   * @return an item relative to the given item's id
   */
  def getItemById(itemId: Int): StorableItem = storableItems(itemId)

  /**
   * Retrieve an item from a given name
   * @param name the name of the item to look for
   * @return an item relative to the given item's name
   */
  def getItemByNamespace(name: String): StorableItem = storableItems find {
    "minecraft:" + _.name == name
  } get

}