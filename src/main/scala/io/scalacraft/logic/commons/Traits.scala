package io.scalacraft.logic.commons

import akka.actor.{Actor, ActorLogging}

object Traits {

  trait EnrichedActor extends Actor with ActorLogging with DefaultTimeout with ImplicitContext

}
