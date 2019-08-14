package io.scalacraft.logic

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props, Timers}
import akka.pattern._
import io.scalacraft.logic.DiggingManager.Message.PlayerDiggingWithItem
import io.scalacraft.logic.Player.Message.CollectItemWithType
import io.scalacraft.logic.PlayerInventoryActor.Message.{AddItem, RetrieveAllItems, RetrieveHeldedItemId, UseHeldedItem}
import io.scalacraft.logic.messages.Message._
import io.scalacraft.logic.traits.{DefaultTimeout, ImplicitContext}
import io.scalacraft.misc.ServerConfiguration
import io.scalacraft.packets.DataTypes.{Position, SlotData}
import io.scalacraft.packets.clientbound.PlayPackets._
import io.scalacraft.packets.clientbound.{PlayPackets => cb}
import io.scalacraft.packets.serverbound.PlayPackets._
import io.scalacraft.packets.serverbound.{PlayPackets => sb}
import net.querz.nbt.CompoundTag
import net.querz.nbt.mca.MCAUtil

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Random, Success}

class Player(username: String, userContext: ActorRef, serverConfiguration: ServerConfiguration) extends Actor
  with Timers with ActorLogging with DefaultTimeout with ImplicitContext {

  import Player._

  private val world = context.parent
  private val inventory = context.actorOf(PlayerInventoryActor.props(self))

  private val playerId = 0
  private val worldDimension = WorldDimension.Overworld
  private var posX: Int = 5
  private var posY: Int = 63
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
      userContext ! JoinGame(playerId, serverConfiguration.gameMode, worldDimension,
        serverConfiguration.serverDifficulty, serverConfiguration.maxPlayers, serverConfiguration.levelTypeBiome,
        serverConfiguration.debug)
      world ! JoiningGame
    case ClientSettings(locale, viewDistance, _, _, _, mainHand) =>
      this.locale = locale
      this.mainHand = mainHand
      this.viewDistance = viewDistance

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
    case sb.KeepAlive(keepAliveId) if keepAliveId == lastKeepAliveId =>
      timers.cancel(KeepAliveTimeoutKey)
    case sb.KeepAlive(keepAliveId) if keepAliveId != lastKeepAliveId =>
      log.warning("Client keep alive id invalid.")
    case sb.Player(onGround) =>
      this.onGround = onGround
    case sb.PlayerPosition(x, feetY, z, onGround) =>
      this.posX = x.toInt
      this.posY = feetY.toInt
      this.posZ = z.toInt
      this.onGround = onGround
    case sb.PlayerPositionAndLook(x, feetY, z, yaw, pitch, onGround) =>
      this.posX = x.toInt
      this.posY = feetY.toInt
      this.posZ = z.toInt
      this.yaw = yaw
      this.pitch = pitch
      this.onGround = onGround
    case sb.PlayerLook(yaw, pitch, onGround) =>
      this.yaw = yaw
      this.pitch = pitch
      this.onGround = onGround
    case msg: sb.HeldItemChange => inventory.forward(msg)
    case playerDigging@PlayerDigging(status, position, face) =>
      digging = Some(status)
      (inventory ? RetrieveHeldedItemId) map(_.asInstanceOf[Option[Int]]) onComplete {
        case Success(heldedItemId) =>
          world ! PlayerDiggingWithItem(playerId, playerDigging, heldedItemId)
          log.info("User: {} {} digging", username, status)
        case Failure(exception) => log.warning("Failed to retrieve helded item.")
      }
    case msg: PlayerBlockPlacement =>
      (inventory ? UseHeldedItem).map(_.asInstanceOf[Int]) onComplete {
        case Success(itemId) =>
          world ! BlockPlacedByUser(msg, itemId, username)
        case Failure(exception) => log.warning("Failed to retrieve placed block type.")
      }
    case animation: sb.Animation =>
      world ! PlayerAnimation(username, playerId, animation)
    case ForwardToClient(msg) => msg match {
      case CollectItemWithType(collectItem, blockStateId) =>
        userContext.forward(collectItem)
        if (collectItem.collectorEntityId == playerId) {
          (inventory ? AddItem(InventoryItem(blockStateId, collectItem.pickUpItemCount)))
            . map(_.asInstanceOf[List[Option[InventoryItem]]]) onComplete {
            case Success(list) =>
              list.collect {
                case Some(item) =>
                  val slot = list.indexOf(Some(item))
                  val slotData = Some(SlotData(item.itemId, item.quantity, new CompoundTag()))
                  userContext ! SetSlot(0, slot, slotData)
              }
            case Failure(exception) => log.warning("Failed to add item to inventory.")
          }
        }
      case _ => userContext ! msg
    }
    case timeUpdate: TimeUpdate => userContext forward timeUpdate
    case RemovePlayer =>
      world ! LeavingGame
      self ! PoisonPill
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

    case class CollectItemWithType(collectItem: CollectItem, blockStateId: Int) extends Message

  }

  private case object KeepAliveTickKey

  private case object KeepAliveTick

  private case object KeepAliveTimeout

  private case object KeepAliveTimeoutKey

  def props(username: String, userContext: ActorRef, serverConfiguration: ServerConfiguration): Props =
    Props(new Player(username, userContext, serverConfiguration))

  def name(username: String): String = s"Player-$username"

}
