package io.scalacraft.logic.creatures

import java.io.FileInputStream

import alice.tuprolog.{Struct, Term, Theory}
import io.scalacraft.logic.commons.ScalaToProlog._
import io.scalacraft.core.packets.DataTypes.Position

class ComputeCreatureMoves() {

  val FileName = "src/main/prolog/computeMoves.pl"
  private val engine = mkPrologEngine(new Theory(new FileInputStream(FileName)))

  implicit def termToPosition(terms: List[Term]): List[Position] = terms.map { term =>
    import alice.tuprolog.Int

    val positionStructure = term.asInstanceOf[Struct]
    val x = positionStructure.getTerm(0).asInstanceOf[Int].intValue()
    val y = positionStructure.getTerm(1).asInstanceOf[Int].intValue()
    val z = positionStructure.getTerm(2).asInstanceOf[Int].intValue()
    Position(x, y, z)
  }

  /**
   * Computes creature possible moves using a prolog algorithm.
   * @param assertions the assertions to be made.
   * @param x the entity x position.
   * @param y the entity y position.
   * @param z the entity z position
   * @return the list of possible moves.
   */
  def computeMoves(assertions: String, x: Int, y: Int, z: Int): List[Position] = {
    def assert(): Unit = {
      val toAssert = s"retractall(state(_,_,_,_)),$assertions"
      solveWithSuccess(engine, toAssert)
    }

    assert()
    val goal = "moves(" + x + "," + y + "," + z + "," + "R)"
    solveAllAndGetTerm(engine, goal, "R")
  }

}
