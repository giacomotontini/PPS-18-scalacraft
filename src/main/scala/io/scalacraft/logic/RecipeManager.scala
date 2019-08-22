package io.scalacraft.logic

import io.scalacraft.loaders.Recipes
import io.scalacraft.loaders.Recipes.{RecipeResult, ShapeRecipe, ShapelessRecipe}
import io.scalacraft.logic.inventories.InventoryItem


object RecipeManager {

  private def checkForShapelessMatch(craftingItemSorted: List[Int], recipe: ShapelessRecipe): Boolean = {
    craftingItemSorted == recipe.ingredients.sorted
  }

  private def checkForShapeMatch(craftingItems: List[List[Int]], shaped: ShapeRecipe): Boolean = {

    def checkStartingAt(fromX: Int, fromY: Int): Boolean = {
      var found = true
      var checkedIndex = List[(Int, Int)]()
      for (y <- shaped.inShapeSorted.indices if found) {
         for(x <- shaped.inShapeSorted(y).indices if found) {
           if(fromY + shaped.inShapeSorted.size > craftingItems.size  || fromX + shaped.inShapeSorted(y).size > craftingItems.size || craftingItems(fromY + y)(fromX + x) != shaped.inShapeSorted(y)(x)) {
             found = false
           } else {
             checkedIndex = checkedIndex.+:((fromX + x,fromY + y))
           }
         }
      }
      if(found) { //check if unchecked inventory slot are empty
        val mappedIndex = checkedIndex.map(index => index._2 * craftingItems.size + index._1)
        found = !craftingItems.flatten.zipWithIndex.filter(zippedIndex => !mappedIndex.contains(zippedIndex._2)).exists(_._1 != -1)
      }
      found
    }

    val neededItems = shaped.inShapeSorted
    if(craftingItems.size < neededItems.size || craftingItems.size < neededItems.map(_.size).max) { //check if crafting grid is smaller than necessary (i.e.playerInventory)
      false
    } else {
      (for {y <- craftingItems.indices
            x <- craftingItems.indices
            if checkStartingAt(x, y)
           } yield true).headOption.getOrElse(false)
    }

  }

  def checkForRecipes(craftingItems: List[Option[InventoryItem]]): Option[RecipeResult] = {

    def _checkForRecipes(craftingItems: List[List[Int]]): Option[RecipeResult] = {
      val craftingItemsSorted = craftingItems.flatten.filter(_ != -1).sorted

      (for {recipe <- Recipes.recipes
            alternative <- recipe._2
            crafted = if (alternative.hasShape) {
              val shaped = alternative.asInstanceOf[ShapeRecipe]
              checkForShapeMatch(craftingItems, shaped)

            } else {
              val shapeless = alternative.asInstanceOf[ShapelessRecipe]
              checkForShapelessMatch(craftingItemsSorted, shapeless)
            }
            if crafted
            } yield alternative.result).headOption
    }

    implicit def inventoryNotationConversion(inventory: List[Option[InventoryItem]]) : List[List[Int]] = {
      inventory.map {
        case Some(item) => item.itemId
        case None => -1
      }.grouped(Math.sqrt(inventory.size).toInt).toList
    }

    _checkForRecipes(craftingItems)
  }

}
