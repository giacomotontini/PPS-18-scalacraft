package io.scalacraft.logic

import java.io.FileInputStream

import alice.tuprolog.{Prolog, SolveInfo, Struct, Term, Theory}
import io.scalacraft.Scala2P
import io.scalacraft.packets.DataTypes.Position

private[this] object ScalaToProlog {

  def extractTerm(solveInfo:SolveInfo, s:String): Term =
    solveInfo.getTerm(s)

  implicit def stringToTerm(s: String): Term = Term.createTerm(s)

  implicit def termToPosition(terms: List[Term]): List[Position] = terms.map{term =>
    import alice.tuprolog.Int
    val positionStructure = term.asInstanceOf[Struct]
    val x = positionStructure.getTerm(0).asInstanceOf[Int].intValue()
    val y = positionStructure.getTerm(1).asInstanceOf[Int].intValue()
    val z = positionStructure.getTerm(2).asInstanceOf[Int].intValue()
    Position(x,y,z)
  }

  def mkPrologEngine(theory: Theory): Term => Stream[SolveInfo] = {
    val engine = new Prolog
    engine.setTheory(theory)

    goal => new Iterable[SolveInfo]{

      override def iterator: Iterator[SolveInfo] = new Iterator[SolveInfo]{
        var solution: Option[SolveInfo] = Some(engine.solve(goal))

        override def hasNext: Boolean = solution.isDefined &&
          (solution.get.isSuccess || solution.get.hasOpenAlternatives)

        override def next(): SolveInfo =
          try solution.get
          finally solution = if (solution.get.hasOpenAlternatives) Some(engine.solveNext()) else None
      }
    }.toStream
  }

  def solveWithSuccess(engine: Term => Stream[SolveInfo], goal: Term): Boolean =
    engine(goal).map(_.isSuccess).headOption.contains(true)

  def solveAllAndGetTerm(engine: Term => Stream[SolveInfo], goal: Term, term: String): List[Position] =
    engine(goal) map (extractTerm(_,term)) toList
}

class ComputeCreatureMoves(toAssert: Seq[String]) {
  import Scala2P._
  val FileName = "src/main/resources/computeMoves.pl"
  private val engine = mkPrologEngine(new Theory(new FileInputStream(FileName)))
  assertions(toAssert)
  def assertions(assertions: Seq[String]): Unit = {
    val toAssert = assertions.map(assertion => "assert(" + assertion + ")").reduce((assertion1, assertion2) => assertion1 + "," + assertion2)
    val goal = toAssert
    solveWithSuccess(engine, goal)
  }

  def computeMoves(x: Int, y: Int, z: Int): List[Position] = {
    val goal = "moves(" + x + "," + y + "," + z + "," + "R)"
    solveAllAndGetTerm(engine, goal, "R")
  }
}
