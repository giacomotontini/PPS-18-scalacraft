package io.scalacraft.logic.traits.ai.general

import akka.actor.{Actor, Cancellable, Timers}
import io.scalacraft.logic.commons.{DefaultTimeout, ImplicitContext}
import io.scalacraft.logic.traits.creatures.CreatureParameters
import io.scalacraft.packets.Entities.MobEntity

import scala.concurrent.duration._

trait AI[T <: MobEntity] extends Movement[T] {
  this: CreatureParameters[T] with Actor with Timers with ImplicitContext with DefaultTimeout =>

  var cancellable: Cancellable = _

  override def preStart(): Unit = {
    cancellable = context.system.scheduler.schedule(0 millis, MovementTickPeriod) {
      doMove()
    }
  }

  override def postStop(): Unit = cancellable.cancel()
}
