package io.scalacraft.logic.traits

import io.scalacraft.loaders.Recipes
import io.scalacraft.loaders.Recipes.{RecipeResult, ShapeRecipe, ShapelessRecipe}

object RecipeManager {

  def checkForShapelessMatch(craftingItemSorted: List[Int], recipe: ShapelessRecipe): Boolean = {
    craftingItemSorted == recipe.ingredients.sorted
  }

  def checkForShapeMatch(craftingItems: List[List[Int]], shaped: ShapeRecipe): Boolean = {



    def checkStartingAt(fromX: Int, fromY: Int): Boolean = {
      var found = true
      var checkedIndex = List[(Int, Int)]()
      for (y <- shaped.inShape.indices if found) {
         for(x <- 0 until shaped.inShape.map(_.size).max if found) {
           if(fromY + y >= craftingItems.size  || fromX + x >= craftingItems.size || craftingItems(fromY + y)(fromX + x) != shaped.inShape(y)(x)) {
             found = false
           } else {
             checkedIndex = checkedIndex.+:((fromX + x,fromY + y))
           }
         }
      }
      //check if uncheck slot are empty
      val mappedIndex = checkedIndex.map(index => index._2 * craftingItems.size + index._1)
      found = !craftingItems.flatten.zipWithIndex.filter(zippedIndex => !mappedIndex.contains(zippedIndex._2)).exists(elem => {println(elem); elem._1 != -1})
      found
    }

    val neededItems = shaped.inShape
    if(craftingItems.size < neededItems.size || craftingItems.head.size < neededItems.map(_.size).max) { //check if crafting grid is smaller than necessary (i.e.playerInventory)
      false
    } else {
      (for {y <- craftingItems.indices
            x <- craftingItems.indices
            if checkStartingAt(x, y)
           } yield true).headOption.getOrElse(false)
    }

  }

  def checkForRecipes(craftingItems: List[List[Int]]): List[RecipeResult] = {
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
          } yield alternative.result).toList
  }
}

object test extends App {

  import RecipeManager._

  val myIngredients = List(List(-1,-1,-1), List(-1,13,-1), List(-1, 13, -1))
  println(checkForRecipes(myIngredients).toList)
}
