package io.scalacraft.logic

import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Timers}
import akka.pattern._
import io.scalacraft.loaders.Blocks
import io.scalacraft.logic.PlayerInventoryActor.Message.AddItem
import io.scalacraft.logic.messages.Message.{CanJoinGame, ChunkNotPresent, RequestChunkData}
import io.scalacraft.logic.traits.{DefaultTimeout, ImplicitContext}
import io.scalacraft.misc.{Helpers, ServerConfiguration}
import io.scalacraft.packets.DataTypes.{Angle, Position, SlotData}
import io.scalacraft.packets.Entities
import io.scalacraft.packets.clientbound.PlayPackets.{BlockBreakAnimation, BlockChange, ChunkData, CollectItem, Effect, EffectId, EntityMetadata, JoinGame, SetSlot, SpawnMob, SpawnObject, SpawnPosition, Tag, WorldDimension}
import io.scalacraft.packets.clientbound.{PlayPackets => cb}
import io.scalacraft.packets.serverbound.PlayPackets._
import io.scalacraft.packets.serverbound.{PlayPackets => sb}
import net.querz.nbt.CompoundTag
import net.querz.nbt.mca.MCAUtil

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Random, Success}

class Player(username: String, userContext: ActorRef) extends Actor with ActorLogging
  with Timers with ActorLogging with DefaultTimeout with ImplicitContext {

  import Player._

  private val world = context.parent
  private val inventory = context.actorOf(PlayerInventoryActor.props(self))

  private val entityId = 0
  private val worldDimension = WorldDimension.Overworld
  private var posX: Int = -20
  private var posY: Int = 70
  private var posZ: Int = 20
  private var yaw: Float = 0.0f
  private var pitch: Float = 0.0f
  private var digging: Option[PlayerDiggingStatus] = None
  private var onGround = true
  private var health = 20
  private var foodHealth = 20
  private var mainHand: MainHand = _
  private var locale: String = _
  private var viewDistance: Int = _ //render distance in chunks

  private var lastKeepAliveId: Long = _
  private val randomGenerator = Random

  private def preStartBehaviour: Receive = {
    case CanJoinGame =>
      import ServerConfiguration._
      userContext ! JoinGame(entityId, GameMode, worldDimension, ServerDifficulty, MaxPlayers, LevelTypeBiome,
        ReducedDebugInfo)
    case clientSettings: ClientSettings =>
      locale = clientSettings.locale
      mainHand = clientSettings.mainHand
      viewDistance = clientSettings.viewDistance

      val chunkX = MCAUtil.blockToChunk(posX)
      val chunkZ = MCAUtil.blockToChunk(posZ)
      Future.sequence(for (x <- chunkX - 3 to chunkX + 3; z <- chunkZ - 3 to chunkZ + 3) yield {
        world ? RequestChunkData(x, z) map {
          case chunkData: ChunkData => userContext ! chunkData
          case ChunkNotPresent => log.warning(s"Chunk ($x,$z) not present.")
        }
      }) onComplete {
        case Success(_) =>
          userContext ! SpawnPosition(Position(posX, posY, posZ))

          val teleportId = randomGenerator.nextInt()
          val flags = playerPositionAndLookFlags(xRelative = false, yRelative = false, zRelative = false, Y_ROT = false, X_ROT = false)
          val positionAndLook = cb.PlayerPositionAndLook(posX, posY, posZ, yaw, pitch, flags, teleportId)
          userContext ! positionAndLook

          context.become(confirmTeleport(positionAndLook))
        case Failure(e) => log.error(e, "Cannot load chunks.")
      }
    case clientStatus: ClientStatus if clientStatus.action == ClientStatusAction.PerformRespawn =>
      log.warning("Respawn not to be handled on login.")
    case clientStatus: ClientStatus if clientStatus.action == ClientStatusAction.RequestStats =>
      log.warning("User request statistics, not handled.")
  }

  private def confirmTeleport(positionAndLook: cb.PlayerPositionAndLook): Receive = {
    case teleportConfirm: TeleportConfirm =>
      if (teleportConfirm.teleportId == positionAndLook.teleportId) {
        userContext ! positionAndLook

        timers.startPeriodicTimer(KeepAliveTickKey, KeepAliveTick, 5 seconds)
        context.become(playingBehaviour)
      } else {
        log.warning("Client not confirm teleport.")
      }
  }

  private def playingBehaviour: Receive = {
    case KeepAliveTick =>
      lastKeepAliveId = randomGenerator.nextLong()
      userContext ! cb.KeepAlive(lastKeepAliveId)
      if (!timers.isTimerActive(KeepAliveTimeoutKey)) {
        timers.startSingleTimer(KeepAliveTimeoutKey, KeepAliveTimeout, 30.seconds)
      }
    case KeepAliveTimeout =>
      timers.cancel(KeepAliveTickKey)
    case keepAlive: sb.KeepAlive if keepAlive.keepAliveId == lastKeepAliveId =>
      timers.cancel(KeepAliveTimeoutKey)
    case keepAlive: sb.KeepAlive if keepAlive.keepAliveId != lastKeepAliveId =>
      log.warning("Client keep alive id invalid.")
    case player: Player =>
      onGround = player.onGround
    case playerPosition: PlayerPosition =>
      posX = playerPosition.x.toInt
      posY = playerPosition.feetY.toInt
      posZ = playerPosition.z.toInt
      onGround = playerPosition.onGround
    case playerPositionAndLook: sb.PlayerPositionAndLook =>
      posX = playerPositionAndLook.x.toInt
      posY = playerPositionAndLook.feetY.toInt
      posZ = playerPositionAndLook.z.toInt
      yaw = playerPositionAndLook.yaw
      pitch = playerPositionAndLook.pitch
      onGround = playerPositionAndLook.onGround
    case playerLook: PlayerLook =>
      yaw = playerLook.yaw
      pitch = playerLook.pitch
      onGround = playerLook.onGround
    case playerDigging: PlayerDigging =>
      digging = Some(playerDigging.status)
      if (playerDigging.status == PlayerDiggingStatus.StartedDigging) {
        log.info("User:", username + " started digging")
      } else if(playerDigging.status == PlayerDiggingStatus.CancelledDigging) {
        log.info("User:", username + " canceled digging")
      } else if (playerDigging.status == PlayerDiggingStatus.FinishedDigging) {
        log.info("User:", username + " finished digging")
        val pos = playerDigging.location
        val itemEntityId: Int = randomGenerator.nextInt()
        val item = new Entities.Item()
        item.item = Some(SlotData(4, 1, new CompoundTag()))


        userContext ! Effect(EffectId.BlockBreakWithSound, playerDigging.location, 0, false)
        userContext ! BlockBreakAnimation(entityId, playerDigging.location, 10)
        userContext ! SpawnObject(itemEntityId, UUID.randomUUID(), 2, pos.x, pos.y, pos.z, Angle(0), Angle(0), 1, 0, 0, 0)
        userContext ! EntityMetadata(itemEntityId, item)
        userContext ! BlockChange(playerDigging.location, 0) //air
        Thread.sleep(500)  // TODO: remove
        self ! CollectItem(itemEntityId, entityId, 1) //TODO to be changed with a internal message wolrd -> player
      }
    case collectItem: CollectItem  =>
      //add to my inventory
      userContext.forward(collectItem)

      if (collectItem.collectorEntityId == entityId) {
        inventory ? AddItem(InventoryItem(2, collectItem.pickUpItemCount)) onComplete {
          case Success(list: List[Option[InventoryItem]]) =>
            list.collect {
            case Some(item) =>
              println(item)
              val slot = list.indexOf(Some(item))
              val slotData = Some(SlotData(item.itemId, item.quantity, new CompoundTag()))
              userContext ! SetSlot(0, slot, slotData)
          }
        }
      }


    case heldItemChange: HeldItemChange => println("holding item in slot", heldItemChange.slot)
    case anyOther => println("Not handled", anyOther)

  }

  override def receive: Receive = preStartBehaviour

  private def playerPositionAndLookFlags(xRelative: Boolean, yRelative: Boolean, zRelative: Boolean, Y_ROT: Boolean, X_ROT: Boolean): Byte = {
    var flags: Byte = 0x00
    if (xRelative) flags = (flags | 0x01).toByte
    if (yRelative) flags = (flags | 0x02).toByte
    if (zRelative) flags = (flags | 0x04).toByte
    if (Y_ROT) flags = (flags | 0x08).toByte
    if (X_ROT) flags = (flags | 0x10).toByte
    flags
  }

}

object Player {

  sealed trait Message
  object Message {
    case class CollectItemWithType(collectItem: CollectItem, itemId: Int) extends Message
  }

  private case object KeepAliveTickKey

  private case object KeepAliveTick

  private case object KeepAliveTimeout

  private case object KeepAliveTimeoutKey

  def props(username: String, userContext: ActorRef): Props = Props(new Player(username, userContext))

  def name(username: String): String = s"Player-$username"

}
