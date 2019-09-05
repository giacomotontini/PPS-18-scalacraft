package io.scalacraft.logic.creatures.behaviours

import akka.actor.Cancellable
import io.scalacraft.logic.commons.Traits.EnrichedActor
import io.scalacraft.logic.creatures.parameters.CreatureParameters
import io.scalacraft.core.packets.Entities.MobEntity

import scala.concurrent.duration._

trait AI[T <: MobEntity] extends Movement[T] {
  this: CreatureParameters[T] with EnrichedActor =>

  var cancellable: Cancellable = _

  override def preStart(): Unit = {
    // TODO: sistema periodo
    cancellable = context.system.scheduler.schedule(0 millis, MovementTickPeriod) {
      doMove()
    }
  }

  override def postStop(): Unit = cancellable.cancel()

}
