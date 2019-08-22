package io.scalacraft.logic.inventories.traits

import akka.util.Timeout

import scala.concurrent.duration._
import scala.language.postfixOps

trait DefaultTimeout {

  protected implicit val defaultTimeout: Timeout = Timeout(5000 millis)

}
