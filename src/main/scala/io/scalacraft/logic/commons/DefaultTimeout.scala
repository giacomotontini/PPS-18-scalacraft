package io.scalacraft.logic.commons

import akka.util.Timeout

import scala.concurrent.duration._
import scala.language.postfixOps

trait DefaultTimeout {

  protected implicit val defaultTimeout: Timeout = Timeout(5000 millis)

}
