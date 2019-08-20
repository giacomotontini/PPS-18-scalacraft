package io.scalacraft.logic

import io.scalacraft.loaders.Recipes
import io.scalacraft.loaders.Recipes.{ShapeRecipe, ShapelessRecipe}
import org.scalatest.{FlatSpec, Matchers}

import scala.reflect._
import scala.util.Random

case class RecipeManagerSpec() extends FlatSpec with Matchers {

  "A RecipeManager when an inventory has the necessary ingredients for recipe" should "give the correct recipe" in {
    Recipes.recipes.foreach(recipe => {
      (recipe._2 collect {
        case alternative: ShapelessRecipe => alternative
      }).foreach(alternative => {
        val ingredients = alternative.ingredients.map(ingredient => Some(InventoryItem(ingredient, 1))).zipWithIndex
        val myInventory = Array.fill(CraftingTableInventory.CraftingInputSlotRange.length)(Option.empty[InventoryItem])(classTag[Option[InventoryItem]])

        Random.shuffle(ingredients).foreach(a => {
          myInventory(a._2) = a._1
        })
        RecipeManager.checkForRecipes(myInventory.toList) shouldBe (Some(alternative.result))
      })
    })
  }

  "A RecipeManager when an inventory has the necessary items shaped correctly" should "give the correct recipe " in {
    val inventorySize = math.sqrt(CraftingTableInventory.CraftingInputSlotRange.length).toInt
    Recipes.recipes.foreach(recipe => {
      (recipe._2 collect {
        case alternative: ShapeRecipe => alternative
      }).foreach(alternative => {
        val items = alternative.inShape.map(_.map(item => Some(InventoryItem(item, 1))).zipWithIndex).zipWithIndex
        val myInventory = Array.fill(CraftingTableInventory.CraftingInputSlotRange.length)(Option.empty[InventoryItem])(classTag[Option[InventoryItem]])

        items.foreach { row =>
          row._1.foreach(item => {
            myInventory(row._2 * inventorySize + item._2) = item._1
          })
        }
        RecipeManager.checkForRecipes(myInventory.toList) shouldBe Some(alternative.result)
      })
    })
  }
}
