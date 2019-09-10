package io.scalacraft.logic

import io.scalacraft.loaders.Recipes
import io.scalacraft.loaders.Recipes.{RecipeResult, ShapeRecipe, ShapelessRecipe}
import io.scalacraft.logic.inventories.InventoryItem

/**
 * A manager for all game recipes, both shaped and shapeless.
 */
object RecipeManager {

  /**
   * Check if the supplied ingredients matches the required ones (from recipe)
   * @param craftingItemSorted the items from the crafting input area, sorted.
   * @param recipe the shapeless recipe to check against
   * @return the recipe outcome
   */
  private def checkForShapelessMatch(craftingItemSorted: List[Int], recipe: ShapelessRecipe): Boolean =
    craftingItemSorted == recipe.ingredients.sorted

  /**
   * Check if the supplied ingredients matches the required ones and are in the correct position (from recipe)
   * @param craftingItems the items from the crafting area. Row by row
   * @param shaped the shaped recipe to check against
   * @return the recipe outcome
   */
  private def checkForShapeMatch(craftingItems: List[List[Int]], shaped: ShapeRecipe): Boolean = {

    // Check starting at FromX and FromY indexes of the inventory.
    def checkStartingAt(fromX: Int, fromY: Int): Boolean = {
      var found = true
      var checkedIndex = List[(Int, Int)]()
      for (y <- shaped.inShapeSorted.indices if found) {
        for (x <- shaped.inShapeSorted(y).indices if found) {
          if (fromY + shaped.inShapeSorted.size > craftingItems.size ||
            fromX + shaped.inShapeSorted(y).size > craftingItems.size ||
            craftingItems(fromY + y)(fromX + x) != shaped.inShapeSorted(y)(x)) {
            found = false
          } else {
            checkedIndex = checkedIndex.+:((fromX + x, fromY + y))
          }
        }
      }
      if (found) { //check if unchecked inventory slot are empty
        val mappedIndex = checkedIndex.map(index => index._2 * craftingItems.size + index._1)
        found = !craftingItems.flatten.zipWithIndex.filter(zippedIndex => !mappedIndex.contains(zippedIndex._2))
          .exists(_._1 != -1)
      }
      found
    }

    /**
     * Check the recipe starting from all crafting area indexes
     * @return the recipe outcome
     */
    def checkForMatch: Boolean = {
      for (y <- craftingItems.indices; x <- craftingItems.indices) {
        if (checkStartingAt(x, y)) return true
      }
      false
    }

    val neededItems = shaped.inShapeSorted
    //check if crafting grid is smaller than necessary (i.e.playerInventory)
    if (craftingItems.size < neededItems.size || craftingItems.size < neededItems.map(_.size).max) false
    else checkForMatch
  }

  /**
   * Check if crafting area items cook a recipe.
   * @param craftingItems the crafting input items
   * @return the first (hopefully the only) recipe that match
   */
  def checkForRecipes(craftingItems: List[Option[InventoryItem]]): Option[RecipeResult] = {

    def _checkForRecipes(craftingItems: List[List[Int]]): Option[RecipeResult] = {
      val craftingItemsSorted = craftingItems.flatten.filter(_ != -1).sorted

      (for {recipe <- Recipes.recipes
            alternative <- recipe._2
            if (
              if (alternative.hasShape) checkForShapeMatch(craftingItems, alternative.asInstanceOf[ShapeRecipe])
              else checkForShapelessMatch(craftingItemsSorted, alternative.asInstanceOf[ShapelessRecipe])
              )
            } yield alternative.result).headOption
    }

    /*
     * Inventory items it's just a list of items. To check a recipe a row by row structure is needed.
     * All crafting areas are squared.
     */
    implicit def inventoryNotationConversion(inventory: List[Option[InventoryItem]]): List[List[Int]] =
      (inventory.map {
        case Some(item) => item.itemId
        case None => -1
      } grouped Math.sqrt(inventory.size).toInt).toList

    _checkForRecipes(craftingItems)
  }

}
