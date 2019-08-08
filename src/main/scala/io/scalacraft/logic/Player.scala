package io.scalacraft.logic

import akka.actor.{Actor, ActorRef, Props}
import io.scalacraft.core.fsm.ConnectionState.PlayState
import io.scalacraft.core.marshalling.Structure
import io.scalacraft.packets.clientbound.PlayPackets.{JoinGame, LevelType, ServerDifficulties, WorldDimension}

class Player(playState: PlayState) extends Actor {

  var userContext: ActorRef = _

  override def preStart(): Unit = {
    userContext = context.actorOf(UserContext.props(playState))

    userContext ! JoinGame(42, 1, WorldDimension.Overworld, ServerDifficulties.Normal, 20, LevelType.Default, false)
  }

  override def receive: Receive = {
    case message: Structure => println(message)
  }

}

object Player {

  def props(playState: PlayState): Props = Props(new Player(playState))

}
