package io.scalacraft.misc

import io.scalacraft.logic.ChickenImpl
import io.scalacraft.logic.traits.creatures.Creature

object CreatureInstance {
    val creatureInstances: List[Creature] = List(ChickenImpl)
}
