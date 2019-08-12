package io.scalacraft.logic

import akka.actor.{Actor, ActorLogging, Props}
import io.scalacraft.logic.BlockManagerActor.Message.DropItem
import io.scalacraft.packets.DataTypes.Position
class BlockManagerActor extends Actor with ActorLogging {

  private var positionToFloatingItems: Map[Position, Int] = Map()

  override def receive: Receive = {
    ???
  }


}

object BlockManagerActor {

  sealed trait Message
  object Message {
    case class DropItem(entityId: Int, position: Position) extends Message
  }

  def props(): Props = Props()
  def name(): String = s"BlockManager"
}
