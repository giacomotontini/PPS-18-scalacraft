package io.scalacraft.logic

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Timers}
import io.scalacraft.logic.BlockManagerActor.Message.PlayerDiggingWithItem
import io.scalacraft.logic.BlockManagerActor.{BrokenTick, PlayerFinishedDiggingTimeout}
import io.scalacraft.packets.DataTypes.{Angle, Position, SlotData}
import io.scalacraft.packets.Entities
import io.scalacraft.packets.clientbound.PlayPackets.{BlockBreakAnimation, BlockChange, CollectItem, Effect, EffectId, EntityMetadata, SpawnObject}
import io.scalacraft.packets.serverbound.PlayPackets.{PlayerDigging, PlayerDiggingStatus}
import net.querz.nbt.CompoundTag

import scala.concurrent.duration._
import scala.util.Random
class BlockManagerActor(worldContext: ActorRef) extends Actor with ActorLogging with Timers {

  private var positionToFloatingItems: Map[Position, List[Int]] = Map()
  private val  randomGenerator = Random

  override def receive: Receive = {
    case digging: PlayerDiggingWithItem if digging.playerDigging.status == PlayerDiggingStatus.StartedDigging =>
      val estimatedDuration = 1000.millis //todo compute duration based on formulaes
      timers.startSingleTimer(BrokenTick, PlayerFinishedDiggingTimeout(digging.playerId), estimatedDuration)
    case digging: PlayerDiggingWithItem if digging.playerDigging.status == PlayerDiggingStatus.CancelledDigging =>
      timers.cancel(digging.playerId)
    case digging: PlayerDiggingWithItem if digging.playerDigging.status == PlayerDiggingStatus.FinishedDigging =>
      val location = digging.playerDigging.location
      val itemEntityId: Int = randomGenerator.nextInt()
      val item = new Entities.Item()
      item.item = Some(SlotData(4, 1, new CompoundTag()))
      worldContext ! Effect(EffectId.BlockBreakWithSound, location, 0, false)
      worldContext ! BlockBreakAnimation(digging.playerId, location, 10)
      worldContext ! SpawnObject(itemEntityId, UUID.randomUUID(), 2, location.x, location.y, location.z, Angle(0), Angle(0), 1, 0, 0, 0)
      worldContext ! EntityMetadata(itemEntityId, item)
      worldContext ! BlockChange(location, 0)
      //todo when a player is near to a floating items send a CollectItem(itemEntityId, entityId, 1)
  }
}

object BlockManagerActor {

  sealed trait Message
  object Message {
    case class DropItem(entityId: Int, position: Position) extends Message
    case class PlayerDiggingWithItem(playerId: Int, playerDigging: PlayerDigging, itemId: Int)
  }
  case class PlayerFinishedDiggingTimeout(playerId: Int)
  case object BrokenTick

  def props(wolrdContext: ActorRef): Props = Props(new BlockManagerActor(wolrdContext))
  def name(): String = s"BlockManager"
}
