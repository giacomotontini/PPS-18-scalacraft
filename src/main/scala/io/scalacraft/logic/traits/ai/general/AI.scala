package io.scalacraft.logic.traits.ai.general

import akka.actor.{Actor, Timers}
import io.scalacraft.logic.commons.{DefaultTimeout, ImplicitContext}
import io.scalacraft.logic.traits.creatures.CreatureParameters
import io.scalacraft.packets.Entities.MobEntity

trait AI[T<: MobEntity] extends Movement[T] {
  this: CreatureParameters[T] with Actor with Timers with ImplicitContext with DefaultTimeout =>

  override def preStart(): Unit = {
    timers.startPeriodicTimer(AiTimerKey, MoveEntity, MovementTickPeriod)
  }

  def aiBehaviour: Receive = {
    case MoveEntity =>
      timers.cancel(AiTimerKey)
      doMove()
  }

}
