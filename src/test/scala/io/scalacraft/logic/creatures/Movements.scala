package io.scalacraft.logic.creatures

object Movements {
  val topRightMovements = List(
    """
      |assert(state(1,1,0, "noSurface")),
      |assert(state(1,0,0, "surface")),
      |assert(state(1,-1,0, "noSurface")),
      |assert(state(1,-2,0, "noSurface"))
      |""".stripMargin,
    """
      |assert(state(1,1,0, "noSurface")),
      |assert(state(1,0,0, "surface")),
      |assert(state(1,-1,0, "noSurface")),
      |assert(state(1,-2,0, "surface"))
      |""".stripMargin,
    """
      |assert(state(1,1,0, "noSurface")),
      |assert(state(1,0,0, "surface")),
      |assert(state(1,-1,0, "surface")),
      |assert(state(1,-2,0, "noSurface"))
      |""".stripMargin,
    """
      |assert(state(1,1,0, "noSurface")),
      |assert(state(1,0,0, "surface")),
      |assert(state(1,-1,0, "surface")),
      |assert(state(1,-2,0, "surface"))
      |""".stripMargin)

  val allTopMovements: String =
    """
      |assert(state(1,1,0, "noSurface")),
      |assert(state(1,0,0, "surface")),
      |assert(state(1,-1,0, "noSurface")),
      |assert(state(1,-2,0, "noSurface")),
      |
      |assert(state(-1,1,0, "noSurface")),
      |assert(state(-1,0,0, "surface")),
      |assert(state(-1,-1,0, "noSurface")),
      |assert(state(-1,-2,0, "noSurface")),
      |
      |assert(state(0,1,1, "noSurface")),
      |assert(state(0,0,1, "surface")),
      |assert(state(0,-1,1, "noSurface")),
      |assert(state(0,-2,1, "noSurface")),
      |
      |assert(state(0,1,-1, "noSurface")),
      |assert(state(0,0,-1, "surface")),
      |assert(state(0,-1,-1, "noSurface")),
      |assert(state(0,-2,-1, "noSurface"))
      |""".stripMargin

  val bottomRightMovement: String =
    """
      |assert(state(1,1,0, "noSurface")),
      |assert(state(1,0,0, "noSurface")),
      |assert(state(1,-1,0, "noSurface")),
      |assert(state(1,-2,0, "surface"))
      |""".stripMargin

  val allBottomMovements: String =
    """
      |assert(state(1,1,0, "noSurface")),
      |assert(state(1,0,0, "noSurface")),
      |assert(state(1,-1,0, "noSurface")),
      |assert(state(1,-2,0, "surface")),
      |
      |assert(state(-1,1,0, "noSurface")),
      |assert(state(-1,0,0, "noSurface")),
      |assert(state(-1,-1,0, "noSurface")),
      |assert(state(-1,-2,0, "surface")),
      |
      |assert(state(0,1,1, "noSurface")),
      |assert(state(0,0,1, "noSurface")),
      |assert(state(0,-1,1, "noSurface")),
      |assert(state(0,-2,1, "surface")),
      |
      |assert(state(0,1,-1, "noSurface")),
      |assert(state(0,0,-1, "noSurface")),
      |assert(state(0,-1,-1, "noSurface")),
      |assert(state(0,-2,-1, "surface"))
      |""".stripMargin

  val sameLevelRightMovements = List(
    """
      |assert(state(1,1,0, "noSurface")),
      |assert(state(1,0,0, "noSurface")),
      |assert(state(1,-1,0, "surface")),
      |assert(state(1,-2,0, "noSurface"))""".stripMargin,
    """
      |assert(state(1,1,0, "noSurface")),
      |assert(state(1,0,0, "noSurface")),
      |assert(state(1,-1,0, "surface")),
      |assert(state(1,-2,0, "surface"))""".stripMargin,
    """
      |assert(state(1,1,0, "surface")),
      |assert(state(1,0,0, "noSurface")),
      |assert(state(1,-1,0, "surface")),
      |assert(state(1,-2,0, "noSurface"))""".stripMargin,
    """
      |assert(state(1,1,0, "surface")),
      |assert(state(1,0,0, "noSurface")),
      |assert(state(1,-1,0, "surface")),
      |assert(state(1,-2,0, "surface"))
      |""".stripMargin)

  val allSameLevelMovements: String =
    """
      |assert(state(1,1,0, "noSurface")),
      |assert(state(1,0,0, "noSurface")),
      |assert(state(1,-1,0, "surface")),
      |assert(state(1,-2,0, "noSurface")),
      |
      |assert(state(-1,1,0, "noSurface")),
      |assert(state(-1,0,0, "noSurface")),
      |assert(state(-1,-1,0, "surface")),
      |assert(state(-1,-2,0, "noSurface")),
      |
      |assert(state(0,1,1, "noSurface")),
      |assert(state(0,0,1, "noSurface")),
      |assert(state(0,-1,1, "surface")),
      |assert(state(0,-2,1, "noSurface")),
      |
      |assert(state(0,1,-1, "noSurface")),
      |assert(state(0,0,-1, "noSurface")),
      |assert(state(0,-1,-1, "surface")),
      |assert(state(0,-2,-1, "noSurface"))
      |""".stripMargin

  val noRightMovements = List(
    """
      |assert(state(1,1,0, "noSurface")),
      |assert(state(1,0,0, "noSurface")),
      |assert(state(1,-1,0, "noSurface")),
      |assert(state(1,-2,0, "noSurface"))
      |""".stripMargin,
    """
      |assert(state(1,1,0, "surface")),
      |assert(state(1,0,0, "noSurface")),
      |assert(state(1,-1,0, "noSurface")),
      |assert(state(1,-2,0, "noSurface"))
      |""".stripMargin,
    """
      |assert(state(1,1,0, "surface")),
      |assert(state(1,0,0, "noSurface")),
      |assert(state(1,-1,0, "noSurface")),
      |assert(state(1,-2,0, "surface"))
      |""".stripMargin,
    """
      |assert(state(1,1,0, "surface")),
      |assert(state(1,0,0, "surface")),
      |assert(state(1,-1,0, "noSurface")),
      |assert(state(1,-2,0, "noSurface"))
      |""".stripMargin,
    """
      |assert(state(1,1,0, "surface")),
      |assert(state(1,0,0, "surface")),
      |assert(state(1,-1,0, "noSurface")),
      |assert(state(1,-2,0, "surface"))
      |""".stripMargin,
    """
      |assert(state(1,1,0, "surface")),
      |assert(state(1,0,0, "surface")),
      |assert(state(1,-1,0, "surface")),
      |assert(state(1,-2,0, "noSurface"))
      |""".stripMargin,
    """
      |assert(state(1,1,0, "surface")),
      |assert(state(1,0,0, "surface")),
      |assert(state(1,-1,0, "surface")),
      |assert(state(1,-2,0, "surface"))
      |""".stripMargin)
}
