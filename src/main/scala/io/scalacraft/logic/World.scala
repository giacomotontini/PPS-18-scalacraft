package io.scalacraft.logic

import akka.actor.{Actor, Props}
import io.scalacraft.core.fsm.ConnectionState.PlayState
import io.scalacraft.logic.messages.Message.PlayerLogged

class World extends Actor {

  override def receive: Receive = {
    case PlayerLogged(playState: PlayState) =>
      context.actorOf(Player.props(playState))


  }

}

object World {

  def props: Props = Props(new World)

}
