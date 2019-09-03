package io.scalacraft.loaders

import io.circe.generic.auto._
import io.circe.parser
import io.scalacraft.packets.DataTypes.Identifier

import scala.io.Source
import scala.language.postfixOps

object Items {

  case class StorableItem(id: Int,
                          displayName: String,
                          name: String,
                          stackSize: Int,
                          attackSpeed: Float,
                          attackDamage: Float)

  private lazy val storableItems = {
    val content = Source.fromInputStream(getClass.getResourceAsStream("/data/items.json")).mkString
    val Right(items) = parser.decode[List[StorableItem]](content)
    items
  }

  private lazy val itemsTags = storableItems map { item => "minecraft:" + item.id -> List(item.id)} toMap

  def getStorableItemById(itemId: Int): StorableItem = storableItems(itemId)

  def getItemByNamespace(name: String): StorableItem = storableItems find { "minecraft:" + _.name == name } get

  def itemsMap: Map[Identifier, List[Int]] = itemsTags

}
