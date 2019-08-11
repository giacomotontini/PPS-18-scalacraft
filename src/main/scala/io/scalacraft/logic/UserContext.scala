package io.scalacraft.logic

import java.io.DataInputStream

import akka.actor.{Actor, ActorLogging, Props}
import io.scalacraft.core.fsm.ConnectionState.PlayState
import io.scalacraft.core.fsm.ParseListener
import io.scalacraft.core.marshalling.Structure

// TODO: technical debit

class UserContext(playState: PlayState) extends Actor with ActorLogging with ParseListener {

  override def preStart(): Unit = {
    playState.listener = this

  }

  override def receive: Receive = {
    case message: Structure =>
      log.info(s"S → C $message")
      playState.connectionManager.writePacket { outStream =>
        playState.packetManagerClientBound.marshal(message)(outStream)
      }
  }

  override def parsePacket(packetId: Int, buffer: DataInputStream): Unit = {
    val message = playState.packetManagerServerBound.unmarshal(packetId)(buffer)
    log.info(s"C → S $message")
    context.parent ! message
  }

}

object UserContext {

  def props(playState: PlayState): Props = Props(new UserContext(playState))

}
