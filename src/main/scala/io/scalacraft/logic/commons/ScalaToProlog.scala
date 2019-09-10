package io.scalacraft.logic.commons

import alice.tuprolog.{Prolog, SolveInfo, Term, Theory}

object ScalaToProlog {

  def extractTerm(solveInfo: SolveInfo, s: String): Term = solveInfo.getTerm(s)

  implicit def stringToTerm(s: String): Term = Term.createTerm(s)

  def mkPrologEngine(theory: Theory): Term => Stream[SolveInfo] = {
    val engine = new Prolog
    engine.setTheory(theory)

    goal => new Iterable[SolveInfo] {

      override def iterator: Iterator[SolveInfo] = new Iterator[SolveInfo] {
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

  def solveAllAndGetTerm(engine: Term => Stream[SolveInfo], goal: Term, term: String): List[Term] =
    engine(goal) map (extractTerm(_, term)) toList

}