package io.scalacraft.logic.creatures.behaviours

import akka.actor.Cancellable
import io.scalacraft.core.packets.Entities.MobEntity
import io.scalacraft.logic.commons.Traits.EnrichedActor
import io.scalacraft.logic.creatures.parameters.CreatureParameters

import scala.util.{Failure, Success}

trait AI[T <: MobEntity] extends Movement[T] {
  this: CreatureParameters[T] with EnrichedActor =>

  var cancellable: Cancellable = _

  private def scheduleMove(): Unit = {
    cancellable = context.system.scheduler.scheduleOnce(MovementTickPeriod) {
      doMove() onComplete {
        case Success(_) => scheduleMove()
        case Failure(_) => //no movement, silently ignored.
      }
    }
  }

  override def preStart(): Unit = {
    scheduleMove()
  }

  override def postStop(): Unit = cancellable.cancel()
}
