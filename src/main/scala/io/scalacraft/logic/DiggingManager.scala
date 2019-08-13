package io.scalacraft.logic

import java.util.UUID

import akka.pattern._
import akka.actor.{Actor, ActorLogging, ActorRef, Props, Timers}
import io.scalacraft.loaders.Blocks
import io.scalacraft.logic.DiggingManager.Message.PlayerDiggingWithItem
import io.scalacraft.logic.Player.Message.CollectItemWithType
import io.scalacraft.logic.messages.Message.BlockBreakAtPosition
import io.scalacraft.logic.traits.{DefaultTimeout, ImplicitContext}
import io.scalacraft.packets.DataTypes.{Angle, Position, SlotData}
import io.scalacraft.packets.Entities
import io.scalacraft.packets.clientbound.PlayPackets.{CollectItem, EntityMetadata, SpawnObject}
import io.scalacraft.packets.serverbound.PlayPackets.{PlayerDigging, PlayerDiggingStatus}
import net.querz.nbt.CompoundTag

import scala.concurrent.duration._
import scala.util.{Random, Success}
class DiggingManager(worldContext: ActorRef) extends Actor with ActorLogging with Timers with DefaultTimeout with ImplicitContext {

  private var positionToFloatingItems: Map[Position, List[Int]] = Map()
  private val  randomGenerator = Random

  override def receive: Receive = {
    case PlayerDiggingWithItem(playerId, playerDigging, itemId) if playerDigging.status == PlayerDiggingStatus.StartedDigging =>
      //val estimatedDuration = 1000.millis //todo compute duration based on formulaes
      //timers.startSingleTimer(BrokenTick, PlayerFinishedDiggingTimeout(digging.playerId), estimatedDuration)
    case PlayerDiggingWithItem(playerId, playerDigging, itemId) if playerDigging.status == PlayerDiggingStatus.CancelledDigging =>
      //timers.cancel(digging.playerId)
    case PlayerDiggingWithItem(playerId, playerDigging, itemId) if playerDigging.status == PlayerDiggingStatus.FinishedDigging =>
      val position =  playerDigging.position
      worldContext ? BlockBreakAtPosition(position, playerId) onComplete {
        case Success(blockStateId: Int) =>
          val droppedItemIds = Blocks.blockFromStateId(blockStateId).drops
          println(droppedItemIds)
          droppedItemIds.foreach(droppedItemId => {
            val itemEntity = new Entities.Item()
            itemEntity.item = Some(SlotData(droppedItemId, 1, new CompoundTag()))
            val itemEntityId: Int = randomGenerator.nextInt()
            worldContext ! SpawnObject(itemEntityId, UUID.randomUUID(), 2, position.x, position.y, position.z, Angle(0), Angle(0), 1, 0, 0, 0)
            worldContext ! EntityMetadata(itemEntityId, itemEntity)
            Thread.sleep(1000)
            worldContext ! CollectItemWithType(CollectItem(itemEntityId, playerId, 1), droppedItemId)
          })
      }
  }
}

object DiggingManager {

  sealed trait Message
  object Message {
    case class PlayerDiggingWithItem(playerId: Int, playerDigging: PlayerDigging, itemId: Option[Int])
  }
  case class PlayerFinishedDiggingTimeout(playerId: Int)
  case object BrokenTick

  def props(wolrdContext: ActorRef): Props = Props(new DiggingManager(wolrdContext))
  def name(): String = s"DiggingManager"
}
