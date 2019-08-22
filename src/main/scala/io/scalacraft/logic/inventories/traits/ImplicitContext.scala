package io.scalacraft.logic.inventories.traits

import akka.actor.Actor

import scala.concurrent.ExecutionContext

trait ImplicitContext {
  this: Actor =>

  protected implicit val executionContext: ExecutionContext = context.dispatcher

}
