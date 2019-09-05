package io.scalacraft.logic.commons

import akka.util.Timeout

import scala.concurrent.duration._

/**
 * Used as mixin to import the default timeout used in most of the cases.
 */
trait DefaultTimeout {

  protected implicit val defaultTimeout: Timeout = Timeout(5000 millis)

}
