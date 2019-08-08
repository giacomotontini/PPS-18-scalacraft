package io.scalacraft.logic

import java.io.DataInputStream

import akka.actor.{Actor, Props}
import io.scalacraft.core.fsm.ConnectionState.PlayState
import io.scalacraft.core.fsm.ParseListener
import io.scalacraft.core.marshalling.Structure

// TODO: technical debit

class UserContext(playState: PlayState) extends Actor with ParseListener {

  override def preStart(): Unit = {
    playState.listener = this

  }

  override def receive: Receive = {
    case message: Structure =>
      playState.connectionManager.writePacket { outStream =>
        playState.packetManagerClientBound.marshal(message)(outStream)
      }
  }

  override def parsePacket(packetId: Int, buffer: DataInputStream): Unit = {
    val message = playState.packetManagerServerBound.unmarshal(packetId)(buffer)
    context.parent ! message
  }

}

object UserContext {

  def props(playState: PlayState): Props = Props(new UserContext(playState))

}
