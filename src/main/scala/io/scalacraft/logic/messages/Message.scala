package io.scalacraft.logic.messages

import io.scalacraft.core.fsm.ConnectionState.PlayState

trait Message {

}

object Message {

  case class PlayerLogged(playState: PlayState) extends Message


}
