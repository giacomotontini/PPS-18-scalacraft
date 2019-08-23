package io.scalacraft.logic.creatures.misc

import io.scalacraft.logic.creatures.{ChickenActor, CowActor, PigActor, SheepActor}
import io.scalacraft.logic.traits.creatures.Creature

object CreatureInstances {
    val creatureInstances: List[Creature] = List(ChickenActor, PigActor, SheepActor, CowActor)
}
