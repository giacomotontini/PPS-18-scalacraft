package io.scalacraft.loaders

import io.circe.generic.auto._
import io.circe.{Decoder, parser}

import scala.io.Source

/**
 * A loader for crafting recipes.
 */
object Recipes {

  sealed trait Recipe {
    def hasShape: Boolean = this.isInstanceOf[ShapeRecipe]

    def result: RecipeResult
  }

  /**
   * Represent a result for a recipe.
   * @param count the number of items crafted
   * @param id the crafted item id
   */
  case class RecipeResult(count: Int, id: Int)

  /**
   * Represent a recipe withOUT ingredients shape bond
   * @param ingredients the ingredients needed to follow the recipe
   * @param result the recipe result
   */
  case class ShapelessRecipe(ingredients: List[Int], result: RecipeResult) extends Recipe

  /**
   * Represent a recipe with ingredients shape bond
   * @param inShape the ingredients needed to follow the recipe. Grouped row by row
   * @param result the recipe result
   */
  case class ShapeRecipe(inShape: List[List[Int]], result: RecipeResult) extends Recipe {

    // cannot make a getter for "inShape" val since it must be public and named as it is. Parsing would not work otherwise.
    def inShapeSorted: List[List[Int]] = inShape.reverse
  }

  object Recipe {
    implicit val decodeData: Decoder[Recipe] =
      Decoder[ShapelessRecipe].map[Recipe](identity).or(Decoder[ShapeRecipe].map[Recipe](identity))
  }

  /**
   * Bind each craftable item to each recipe that produces it.
   */
  lazy val recipes: Map[String, List[Recipe]] = {
    val content = Source.fromInputStream(getClass.getResourceAsStream("/data/recipes.json")).mkString
    val Right(parsed) = parser.decode[Map[String, List[Recipe]]](content)
    parsed
  }

}
