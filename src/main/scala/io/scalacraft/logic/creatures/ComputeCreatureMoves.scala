package io.scalacraft.logic.creatures

import java.io.FileInputStream

import alice.tuprolog.{Struct, Term, Theory}
import io.scalacraft.logic.commons.ScalaToProlog._
import io.scalacraft.core.packets.DataTypes.Position

class ComputeCreatureMoves(toAssert: Seq[String]) {

  import ComputeCreatureMoves._

  val FileName = "src/main/prolog/computeMoves.pl"
  private val engine = mkPrologEngine(new Theory(new FileInputStream(FileName)))
  assertions(toAssert)

  def assertions(assertions: Seq[String]): Unit = {
    val toAssert = assertions.map(assertion => "assert(" + assertion + ")")
      .reduce((assertion1, assertion2) => assertion1 + "," + assertion2)
    solveWithSuccess(engine, toAssert)
  }

  def computeMoves(x: Int, y: Int, z: Int): List[Position] = {
    val goal = "moves(" + x + "," + y + "," + z + "," + "R)"
    solveAllAndGetTerm(engine, goal, "R")
  }

}

object ComputeCreatureMoves {

  implicit def termToPosition(terms: List[Term]): List[Position] = terms.map { term =>
    import alice.tuprolog.Int

    val positionStructure = term.asInstanceOf[Struct]
    val x = positionStructure.getTerm(0).asInstanceOf[Int].intValue()
    val y = positionStructure.getTerm(1).asInstanceOf[Int].intValue()
    val z = positionStructure.getTerm(2).asInstanceOf[Int].intValue()
    Position(x, y, z)
  }

}
