package io.scalacraft.logic.commons

import akka.actor.{Actor, ActorLogging}

/**
 * Contains some generic traits used in the project.
 */
object Traits {

  /**
   * Join some useful traits related to actor.
   */
  trait EnrichedActor extends Actor with ActorLogging with DefaultTimeout with ImplicitContext

}
