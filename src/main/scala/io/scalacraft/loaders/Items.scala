package io.scalacraft.loaders

import io.circe.parser
import io.circe.generic.auto._
import scala.io.Source

object Items {

  case class StorableItem(id: Int,
                          displayName: String,
                          name: String,
                          stackSize: Int)

   private lazy val storableItems: List[StorableItem] = {
    val content = Source.fromInputStream(getClass.getResourceAsStream("/data/items.json")).mkString
    val Right(items) = parser.decode[List[StorableItem]](content)
     items
  }

  def getStorableItemById(itemId: Int): StorableItem = {
    storableItems(itemId)
  }
}
