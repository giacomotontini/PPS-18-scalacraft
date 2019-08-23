package io.scalacraft.logic.traits.creatures

import akka.actor.ActorRef

trait CommonParameters {
  val SecondInMillisecond = 1000
  val Udm: Double = 0.0000025 // 1/8000/50
  var world: ActorRef = _
}
