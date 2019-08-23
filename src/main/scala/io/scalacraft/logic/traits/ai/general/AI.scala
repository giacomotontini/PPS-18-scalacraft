package io.scalacraft.logic.traits.ai.general

import akka.actor.{Actor, Timers}
import io.scalacraft.logic.commons.{DefaultTimeout, ImplicitContext}
import io.scalacraft.logic.traits.creatures.CreatureParameters

trait AI extends Movement {
  this: CreatureParameters with Actor with Timers with ImplicitContext with DefaultTimeout =>

  override def preStart(): Unit = {
    timers.startPeriodicTimer(AiTimerKey, MoveEntity, MovementTickPeriod)
  }

  def aiBehaviour: Receive = {
    case MoveEntity =>
      timers.cancel(AiTimerKey)
      doMove()
  }

}
