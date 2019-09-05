package io.scalacraft.logic

import akka.actor.{ActorRef, Props}
import akka.pattern._
import io.scalacraft.loaders.Blocks.{Block, BreakingProperties, Drop}
import io.scalacraft.loaders.Items.StorableItem
import io.scalacraft.loaders.{Blocks, Items}
import io.scalacraft.logic.commons.Message._
import io.scalacraft.logic.commons.Traits.EnrichedActor
import io.scalacraft.misc.Helpers._
import io.scalacraft.packets.DataTypes.{BlockStateId, EntityId, Position}
import io.scalacraft.packets.clientbound.PlayPackets.{BlockBreakAnimation, BlockChange, Effect, EffectId}
import io.scalacraft.packets.serverbound.PlayPackets.{PlayerDigging, PlayerDiggingStatus}
import net.querz.nbt.CompoundTag

import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.{Failure, Random, Success}

class DiggingManager(dropManager: ActorRef) extends EnrichedActor {

  import DiggingManager._
  import PlayerDiggingStatus._

  private val world = context.parent
  private val materialAndTool = """(wooden|stone|iron|golden|diamond)_(axe|pickaxe|sword|shovel)""".r
  private val randomGenerator = new Random
  private val breakingBlocks: mutable.Map[Position, BreakingBlock] = mutable.Map()
  private val fluidExecutor = ExecutionContext.fromExecutor((task: Runnable) => task.run())

  override def receive: Receive = {

    case PlayerDiggingHoldingItem(playerId, _, PlayerDigging(StartedDigging, blockPosition, _), brokeToolId) =>
      ((world ? RequestBlockState(blockPosition)) zip (world ? RequestEntityId)).mapTo[(CompoundTag, EntityId)] onComplete {
        case Success((tag, entityId)) =>
          val blockStateId = Blocks.stateIdFromCompoundTag(tag)
          val block = Blocks.blockFromStateId(blockStateId)
          val bProperties = breakingProperties(block, brokeToolId.map(id => Items.getItemById(id)))
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


        (bProperties match {
          case bProperties@Some(BreakingProperties(true, true, _)) => itemsToDrop(block, bProperties.get)
          case _ => Map.empty
        }) foreach {
          case (dropItemId, quantity) =>
            dropManager ! DropItems(dropItemId, quantity, blockPosition, playerId, playerPosition)
        }

        world ! SendToAllExclude(playerId,
          Effect(EffectId.BlockBreakWithSound, blockPosition, blockStateId, disableRelativeVolume = false))
        world ! BlockBrokenAtPosition(blockPosition)
        world ! ChangeBlockState(blockPosition, AirTag)
        world ! SendToAll(BlockChange(blockPosition, Blocks.stateIdFromCompoundTag(AirTag)))
        slideFluid(blockPosition, AirTag)
      }

  }

  private def breakingProperties(block: Block, brokeTool: Option[StorableItem]): Option[BreakingProperties] = {
    val (material, tool) = brokeTool map {
      _.name match {
        case materialAndTool(_, "sword") => ("sword", "sword")
        case materialAndTool(material, tool) => (material, tool)
        case "shears" => ("shears", "shears")
        case _ => ("hand", "any")
      }
    } getOrElse("hand", "any")

    if (block.tool.isDefined && block.tool.get == tool) {
      if (block.breaking.contains(material)) Some(block.breaking(material)) else None
    } else {
      if (block.breaking.contains("hand")) Some(block.breaking("hand")) else None
    }
  }

  private def itemsToDrop(block: Block, breakingProperties: BreakingProperties): Map[Int, Int] = block.drops map {
    case Drop(Some(min), Some(max), namespace, _) => namespace -> randomGenerator.nextInt(math.round(max - min) + 1)
    case Drop(Some(min), _, namespace, _) => namespace -> math.round(min)
    case Drop(_, _, namespace, _) => namespace -> 1
  } map { case (namespace, quantity) => Items.getItemByNamespace(namespace).id -> quantity } toMap


  private def slideFluid(position: Position, tag: CompoundTag): Unit = {
    implicit val executor: ExecutionContextExecutor = fluidExecutor

    context.system.scheduler.scheduleOnce(250 millis) {
      Future.sequence(for (Position(rx, ry, rz) <- RelativeNears) yield {
        val pos = Position(position.x + rx, position.y + ry, position.z + rz)
        (world ? RequestBlockState(pos)).mapTo[CompoundTag] map { _tag => TagWithPosition(_tag, pos)}
      }) onComplete {
        case Success(bottom :: top :: north :: south :: west :: east :: Nil) =>
          var newTag: Option[CompoundTag] = None

          if (top.tag.isFluid) {
            newTag = Some(top.tag.withFluidLevel(_ => MaxLevel))
          } else {
            val nearFluids = List(north, south, west, east).filter { _.tag.isFluid }
            if (nearFluids.nonEmpty) {
              val higherFluid = nearFluids maxBy { _.tag.fluidLevel }
              if (higherFluid.tag.fluidLevel > (if (tag.isFluid) tag.fluidLevel else MinLevel) + 1) {
                newTag = Some(higherFluid.tag.withFluidLevel(_ - 1))
              }
            }
          }

          if (newTag.isDefined && newTag.get != tag) {
            world ! ChangeBlockState(position, newTag.get)
            world ! SendToAll(BlockChange(position, Blocks.stateIdFromCompoundTag(newTag.get)))

            for (elem <- List(bottom, north, south, west, east) if elem.tag.isFluid || elem.tag == AirTag) {
              slideFluid(elem.position, elem.tag)
            }
          }
        case Success(_) => // never happens
        case Failure(ex) => log.error(ex, "Failed to retrieve block states in slideFluids")
      }
    }
  }

}

object DiggingManager {

  private val FluidsList = List("minecraft:water", "minecraft:lava")
  private val AirTag = Blocks.defaultCompoundTagFromName("air").get
  private val MaxLevel = 8
  private val MinLevel = 0

  private case class BreakingBlock(entityId: EntityId, blockStateId: BlockStateId, block: Block,
                                   breakingProperties: Option[BreakingProperties])

  private case class TagWithPosition(tag: CompoundTag, position: Position)

  private implicit class FluidCompoundTag(compoundTag: CompoundTag) {
    def isFluid: Boolean = compoundTag != null && FluidsList.contains(compoundTag.getString("Name"))
    def fluidLevel: Int = {
      val level = compoundTag.getCompoundTag("Properties").getString("level").toInt
      MaxLevel - level
    }
    def withFluidLevel(f: Int => Int): CompoundTag = {
      val clone = compoundTag.clone()
      val level = f(fluidLevel)
      clone.getCompoundTag("Properties").getStringTag("level")
        .setValue((MaxLevel - level).toString)
      clone
    }
  }

  def props(dropManager: ActorRef): Props = Props(new DiggingManager(dropManager))

  def name: String = "DiggingManager"

}
