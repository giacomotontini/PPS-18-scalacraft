package io.scalacraft.logic.commons

import akka.actor.Actor

import scala.concurrent.ExecutionContext

/**
 * Used as mixin to import the default execution context used in most of the cases.
 */
trait ImplicitContext {
  this: Actor =>

  protected implicit val executionContext: ExecutionContext = context.dispatcher

}
