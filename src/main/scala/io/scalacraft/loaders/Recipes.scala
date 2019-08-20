package io.scalacraft.loaders

import io.circe.{Decoder, parser}
import io.circe.generic.auto._

import scala.io.Source

object Recipes {

  sealed trait Recipe {
    def hasShape: Boolean = this.isInstanceOf[ShapeRecipe]
    def result: RecipeResult
  }
  case class RecipeResult(count: Int, id: Int)
  case class ShapelessRecipe(ingredients: List[Int], override val result: RecipeResult) extends Recipe
  case class ShapeRecipe(inShape: List[List[Int]], override val result: RecipeResult) extends Recipe

  object Recipe {
    implicit val decodeData: Decoder[Recipe] =
      Decoder[ShapelessRecipe].map[Recipe](identity)
      .or(Decoder[ShapeRecipe].map[Recipe](identity))
  }

  lazy val recipes: Map[String, List[Recipe]] = {
    val content = Source.fromInputStream(getClass.getResourceAsStream("/data/recipes.json")).mkString
    val Right(parsed) = parser.decode[Map[String, List[Recipe]]](content)
    parsed
  }
}
