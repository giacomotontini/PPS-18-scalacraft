package io.scalacraft.logic.creatures

import io.scalacraft.logic.creatures.Spawnables.SpawnableCreature
import io.scalacraft.logic.creatures.actors.{ChickenActor, CowActor, PigActor, SheepActor}

object CreatureInstances {
  val creatureInstances: List[SpawnableCreature] = List(ChickenActor, PigActor, SheepActor, CowActor)
}
