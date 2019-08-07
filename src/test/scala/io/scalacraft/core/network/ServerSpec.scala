package io.scalacraft.core.network

import io.scalacraft.misc.ServerConfiguration
import org.scalatest.{FlatSpec, Matchers}

class ServerSpec extends FlatSpec with Matchers {
  var server: Server = _

  "A server" should "receive the correct message" in {
    val payload = "0102"
    server = new Server(ServerConfiguration.PORT, () => new DummyServerHandler(1,payload,2))
    server.run()
    ClientHelper(Some("0301"+payload), None).run()
    Thread.sleep(500)
    server.stop()
  }
  /*"A server" should "receive the correct empty message" in {
    server = new Server(ServerConfiguration.PORT, () => new DummyServerHandler(0,"",0))
    server.run()
    ClientHelper(Some("0100"), None).run()
    Thread.sleep(500)
    server.stop()
  }*/

}
