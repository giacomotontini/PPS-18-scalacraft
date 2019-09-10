package io.scalacraft.logic.creatures

import io.scalacraft.core.packets.DataTypes.Position
import org.scalatest.{FlatSpec, Matchers}

class ComputeCreatureMovesSpec extends FlatSpec with Matchers {
  import Movements._
  val computeCreatureMoves = new ComputeCreatureMoves()
  val posX, posY, posZ = 0

  "A method that compute creature moves" should "get a correct top right movement" in {
    topRightMovements.foreach {
      movement =>
        computeCreatureMoves.computeMoves(movement, posX, posY, posZ) shouldBe List(Position(1, 1, 0))
    }
  }

  it should "get all correct top movements" in {
    computeCreatureMoves.computeMoves(allTopMovements, posX, posY, posZ) shouldBe List(Position(1, 1, 0),
      Position(-1, 1, 0), Position(0, 1, 1), Position(0, 1, -1))
  }

  it should "get a correct bottom right movement" in {
    computeCreatureMoves.computeMoves(bottomRightMovement, posX, posY, posZ) shouldBe List(Position(1, -1, 0))
  }

  it should "get all correct bottom movements" in {
    computeCreatureMoves.computeMoves(allBottomMovements, posX, posY, posZ) shouldBe List(Position(1, -1, 0),
      Position(-1, -1, 0), Position(0, -1, 1), Position(0, -1, -1))
  }

  it should "get a correct same level right movement" in {
    sameLevelRightMovements.foreach {
      movement =>
        computeCreatureMoves.computeMoves(movement, posX, posY, posZ) shouldBe List(Position(1, 0, 0))
    }
  }

  it should "get all correct same level movements" in {
    computeCreatureMoves.computeMoves(allSameLevelMovements, posX, posY, posZ) shouldBe List(Position(1, 0, 0),
      Position(-1, 0, 0), Position(0, 0, 1), Position(0, 0, -1))
  }

  it should "get no right movements" in {
    noRightMovements.foreach {
      movement =>
        computeCreatureMoves.computeMoves(movement, posX, posY, posZ) shouldBe List()
    }
  }

}
