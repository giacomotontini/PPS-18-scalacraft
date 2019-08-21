package io.scalacraft.logic

import java.util.UUID

import akka.actor.{Actor, ActorLogging, Props, Timers}
import akka.pattern._
import io.scalacraft.logic.messages.Message._
import io.scalacraft.logic.traits.{DefaultTimeout, ImplicitContext}
import io.scalacraft.misc.Helpers
import io.scalacraft.packets.DataTypes.{EntityId, Position, SlotData}
import io.scalacraft.packets.Entities
import io.scalacraft.packets.clientbound.PlayPackets.{CollectItem, EntityMetadata, SpawnObject}
import net.querz.nbt.CompoundTag

import scala.collection.mutable
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

class DropManager extends Actor with ActorLogging with Timers with DefaultTimeout with ImplicitContext {

  import DropManager._

  private val world = context.parent
  private val floatingItems: mutable.Map[Position, Map[EntityId, SlotData]] = mutable.Map()

  override def receive: Receive = {

    case DropItems(itemId, quantity, blockPosition, playerId, playerPosition) =>
      ((world ? RequestEntityId) zip (world ? FindFirstSolidBlockPositionUnder(blockPosition))).mapTo[(EntityId, Position)]
      .onComplete {
        case Success((entityId, firstSolidPosition)) =>
          val itemEntity = new Entities.Item()
          val slotData = SlotData(itemId, quantity, new CompoundTag())
          itemEntity.item = Some(slotData)

          world ! SendToAll(spawnObject(entityId, blockPosition.x + 0.5, blockPosition.y, blockPosition.z + 0.5))
          world ! SendToAll(EntityMetadata(entityId, itemEntity))

          val firstDropPosition = firstSolidPosition.withY(_ + 1)
          if (firstDropPosition ~ playerPosition < magnetThreshold) { // if player is near collect auto collect
            context.system.scheduler.scheduleOnce(250 millis) {
              world ! SendToAll(CollectItemWithType(CollectItem(entityId, playerId, quantity), itemId))
            }
          } else {
            val droppedItems = floatingItems.getOrElse(firstDropPosition, Map())
            floatingItems(firstDropPosition) = droppedItems + (entityId -> slotData)
          }
        case Failure(ex) => log.error(ex, "Unable to complete DropItems request")
      }

    case pm @ PlayerMoved(playerId, Position(px, py, pz)) =>
      for (x <- -magnetRadius to magnetRadius;
           y <- -magnetRadius to magnetRadius;
           z <- -magnetRadius to magnetRadius) {
        val position = Position(px + x, py + y + 2, pz + z)

        if (position ~ pm.position < magnetThreshold && floatingItems.contains(position)) {
          val droppedItems = floatingItems.remove(position).get
          droppedItems foreach {
            case (entityId, slotData) =>
              world ! SendToAll(CollectItemWithType(CollectItem(entityId, playerId, slotData.itemCount), slotData.itemId))
          }
        }
      }

    case BlockBrokenAtPosition(position) =>
      val itemsPosition = position.withY(_ + 1)
      if (floatingItems.contains(itemsPosition)) {
        (world ? FindFirstSolidBlockPositionUnder(position)).mapTo[Position] onComplete {
          case Success(firstSolidPosition) =>
            floatingItems(firstSolidPosition.withY(_ + 1)) = floatingItems.remove(itemsPosition).get
          case Failure(ex) => log.error(ex, "Unable to process BlockBrokenAtPosition")
        }
      }
  }

  private def spawnObject(entityId: EntityId, x: Double, y: Double, z: Double): SpawnObject =
    SpawnObject(entityId, UUID.randomUUID(), tpe = 2, x, y, z, data = 1, yaw = Helpers.randomAngle,
      velocityX = Helpers.randomVelocity / 4, velocityY = Helpers.randomVelocity, velocityZ = Helpers.randomVelocity / 4)

}

object DropManager {

  private val magnetThreshold = 2
  private val magnetRadius = 4

  def props: Props = Props(new DropManager)
  def name: String = "DropManager"

}


