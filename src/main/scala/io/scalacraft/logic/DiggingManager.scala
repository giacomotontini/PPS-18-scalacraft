package io.scalacraft.logic

import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable, Props, Timers}
import akka.pattern._
import io.scalacraft.loaders.Blocks.{Block, BreakingProperties, Drop}
import io.scalacraft.loaders.Items.StorableItem
import io.scalacraft.loaders.{Blocks, Items}
import io.scalacraft.logic.DiggingManager.BreakingBlock
import io.scalacraft.logic.messages.Message._
import io.scalacraft.logic.traits.{DefaultTimeout, ImplicitContext}
import io.scalacraft.packets.DataTypes.{BlockStateId, EntityId, ItemId, Position}
import io.scalacraft.packets.clientbound.PlayPackets.{BlockBreakAnimation, BlockChange, Effect, EffectId}
import io.scalacraft.packets.serverbound.PlayPackets.{PlayerDigging, PlayerDiggingStatus}
import net.querz.nbt.CompoundTag

import scala.collection.mutable
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Random, Success}

class DiggingManager(dropManager: ActorRef) extends Actor
  with ActorLogging with Timers with DefaultTimeout with ImplicitContext {

  import PlayerDiggingStatus._

  private val world = context.parent
  private val materialAndTool = """(wooden|stone|iron|golden|diamond)_(axe|pickaxe|sword|shovel)""".r
  private val randomGenerator = new Random
  private val breakingBlocks: mutable.Map[Position, BreakingBlock] = mutable.Map()

  override def receive: Receive = {

    case PlayerDiggingHoldingItem(playerId, _, PlayerDigging(StartedDigging, blockPosition, _), brokeToolId) =>
      ((world ? RequestBlockState(blockPosition)) zip (world ? RequestEntityId)).mapTo[(CompoundTag, EntityId)] onComplete {
        case Success((tag, entityId)) =>
          val blockStateId = Blocks.stateIdFromCompoundTag(tag)
          val block = Blocks.blockFromStateId(blockStateId)
          val bProperties = breakingProperties(block, brokeToolId.map(id => Items.getStorableItemById(id)))
          if (bProperties.isDefined && bProperties.get.value > 0) {
            breakingBlocks(blockPosition) = BreakingBlock(entityId, blockStateId, block, bProperties)

            val stageInterval = bProperties.get.value * 100 // * 1000 / 10
            def showBreakAnimation(destroyStage: Int): Unit = {
              context.system.scheduler.scheduleOnce(stageInterval millis) {
                if (breakingBlocks.contains(blockPosition) && destroyStage <= 10) {
                  world ! SendToAllExclude(playerId, BlockBreakAnimation(entityId, blockPosition, destroyStage))
                  showBreakAnimation(destroyStage + 1)
                }
              }
            }

            showBreakAnimation(1)
          }
        case Failure(ex) => log.error(ex, "Unable to complete PlayerDiggingWithItem request")
      }

    case PlayerDiggingHoldingItem(playerId, _, PlayerDigging(CancelledDigging, blockPosition, _), _) =>
      if (breakingBlocks.contains(blockPosition)) {
        val entityId = breakingBlocks.remove(blockPosition).get.entityId
        world ! SendToAllExclude(playerId, BlockBreakAnimation(entityId, blockPosition, -1))
      }

    case PlayerDiggingHoldingItem(playerId, playerPosition, PlayerDigging(FinishedDigging, blockPosition, _), _) =>
      if (breakingBlocks.contains(blockPosition)) {
        val BreakingBlock(_, blockStateId, block, bProperties) = breakingBlocks.remove(blockPosition).get
        world ! BreakBlockAtPosition(blockPosition)

        (bProperties match {
          case bProperties @ Some(BreakingProperties(true, true, _)) => itemsToDrop(block, bProperties.get)
          case _ => Map.empty
        }) foreach {
          case (dropItemId, quantity) =>
            dropManager ! DropItems(dropItemId, quantity, blockPosition, playerId, playerPosition)
        }

        world ! SendToAll(Effect(EffectId.BlockBreakWithSound, blockPosition, blockStateId, disableRelativeVolume = false))
        world ! SendToAll(BlockChange(blockPosition, 0))
      }

  }

  private def breakingProperties(block: Block, brokeTool: Option[StorableItem]): Option[BreakingProperties] = {
    val (material, tool) = brokeTool map { _.name match {
        case materialAndTool(_, "sword") => ("sword", "sword")
        case materialAndTool(material, tool) => (material, tool)
        case "shears" => ("shears", "shears")
        case _ => ("hand", "any")
      }
    } getOrElse ("hand", "any")

    if (block.tool.isDefined && block.tool.get == tool) {
      if (block.breaking.contains(material)) Some(block.breaking(material)) else None
    } else {
      if (block.breaking.contains("hand")) Some(block.breaking("hand")) else None
    }
  }

  private def itemsToDrop(block: Block, breakingProperties: BreakingProperties): Map[ItemId, Int] = block.drops map {
    case Drop(Some(min), Some(max), namespace, _) => namespace -> randomGenerator.nextInt(math.round(max - min) + 1)
    case Drop(Some(min), _, namespace, _) => namespace -> math.round(min)
    case Drop(_, _, namespace, _) => namespace -> 1
  } map { case (namespace, quantity) => Items.getItemByNamespace(namespace).id -> quantity } toMap

}

object DiggingManager {

  private case class BreakingBlock(entityId: EntityId, blockStateId: BlockStateId, block: Block,
                                   breakingProperties: Option[BreakingProperties])

  def props(dropManager: ActorRef): Props = Props(new DiggingManager(dropManager))
  def name: String = "DiggingManager"

}
