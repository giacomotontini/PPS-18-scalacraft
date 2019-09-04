package io.scalacraft.logic.commons

import akka.util.Timeout

import scala.concurrent.duration._

trait DefaultTimeout {

  protected implicit val defaultTimeout: Timeout = Timeout(5000 millis)

}
