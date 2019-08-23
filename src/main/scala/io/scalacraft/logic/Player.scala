package io.scalacraft.logic

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Stash, Timers}
import akka.pattern._
import io.scalacraft.logic.PlayerInventoryActor.Message.{AddItem, RetrieveHeldItemId, UseHeldItem}
import io.scalacraft.logic.messages.Message._
import io.scalacraft.logic.traits.{DefaultTimeout, ImplicitContext}
import io.scalacraft.misc.ServerConfiguration
import io.scalacraft.packets.DataTypes.{Angle, ItemId, Position}
import io.scalacraft.packets.Entities
import io.scalacraft.packets.clientbound.PlayPackets._
import io.scalacraft.packets.clientbound.{PlayPackets => cb}
import io.scalacraft.packets.serverbound.PlayPackets._
import io.scalacraft.packets.serverbound.{PlayPackets => sb}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Random, Success}

class Player(username: String, playerUUID: UUID, serverConfiguration: ServerConfiguration) extends Actor
  with Timers with ActorLogging with DefaultTimeout with ImplicitContext with Stash {

  import Player._

  private val world = context.parent
  private val inventory = context.actorOf(PlayerInventoryActor.props(self))
  private var userContext: ActorRef = _
  private val randomGenerator = Random

  private var playerEntityId = 0
  private val worldDimension = WorldDimension.Overworld
  private var posX: Double = 548
  private var posY: Double = 69
  private var posZ: Double = -408
  private var yaw: Float = 0.0f
  private var pitch: Float = 0.0f
  private var onGround = true
  private var health = 20
  private var foodHealth = 20
  private var mainHand: MainHand = _
  private var locale: String = _
  private var viewDistance: Int = _ //render distance in chunks
  private val entityMetadata: Entities.Player = new Entities.Player

  private var lastKeepAliveId: Long = _

  private var loadedChunks: Set[(Int, Int)] = Set()

  override def receive: Receive = preStartBehaviour

  private def preStartBehaviour: Receive = {
    case RequestJoinGame(playerEntityId, playerUserContext) =>
      this.playerEntityId = playerEntityId
      userContext = playerUserContext

      userContext ! JoinGame(playerEntityId, serverConfiguration.gameMode, worldDimension,
        serverConfiguration.serverDifficulty, serverConfiguration.maxPlayers, serverConfiguration.levelTypeBiome,
        serverConfiguration.reducedDebugInfo)
      world ! PlayerJoiningGame(playerEntityId, username)

    case clientSettings: sb.ClientSettings =>
      locale = clientSettings.locale
      mainHand = clientSettings.mainHand
      viewDistance = if (clientSettings.viewDistance > ServerConfiguration.MaxViewDistance)
        ServerConfiguration.MaxViewDistance else clientSettings.viewDistance

      loadChunks() onComplete {
        case Success(_) =>
          val teleportId = randomGenerator.nextInt
          val flags = playerPositionAndLookFlags(xRelative = false, yRelative = false, zRelative = false, Y_ROT = false,
            X_ROT = false)
          val positionAndLook = cb.PlayerPositionAndLook(posX, posY, posZ, yaw, pitch, flags, teleportId)
          entityMetadata.health = health
          userContext ! cb.SpawnPosition(Position(posX, posY, posZ))
          userContext ! positionAndLook

          context.become(confirmTeleport(positionAndLook))
        case Failure(e) => log.error(e, "Cannot load chunks.")
      }

    case ClientStatus(ClientStatusAction.PerformRespawn) => log.warning("Respawn not to be handled on login.")
    case ClientStatus(ClientStatusAction.RequestStats) => log.warning("User request statistics, not handled.")
    case sb.PluginMessage(brand, data) => userContext ! cb.PluginMessage(brand, data)
    case _ => stash()
  }

  private def confirmTeleport(positionAndLook: cb.PlayerPositionAndLook): Receive = {
    case sb.TeleportConfirm(teleportId) if positionAndLook.teleportId == teleportId =>
      val entityProperties = EntityProperties(playerEntityId, PlayerEntityProperties)
      world ! PlayerSpawning(playerEntityId, spawnPlayerPacket, entityProperties)

      timers.startPeriodicTimer(KeepAliveTickKey, KeepAliveTick, 5 seconds)
      context.become(playingBehaviour)
      unstashAll()
    case _ => stash()
  }

  private def playingBehaviour: Receive = {
    case KeepAliveTick =>
      lastKeepAliveId = randomGenerator.nextLong()
      userContext ! cb.KeepAlive(lastKeepAliveId)
      if (!timers.isTimerActive(KeepAliveTimeoutKey)) {
        timers.startSingleTimer(KeepAliveTimeoutKey, KeepAliveTimeout, 30.seconds)
      }

    case KeepAliveTimeout => timers.cancel(KeepAliveTickKey)
    case sb.KeepAlive(keepAliveId) if keepAliveId == lastKeepAliveId => timers.cancel(KeepAliveTimeoutKey)
    case sb.KeepAlive(keepAliveId) if keepAliveId != lastKeepAliveId => log.warning("Client keep alive id invalid.")

    case RequestSpawnPacket => sender ! spawnPlayerPacket

    case sb.Player(_onGround) => updatePositionAndLook(onGround = _onGround)
    case sb.PlayerPosition(x, feetY, z, _onGround) => updatePositionAndLook(Some(x, feetY, z), onGround = _onGround)
    case sb.PlayerPositionAndLook(x, feetY, z, yaw, pitch, onGround) =>
      updatePositionAndLook(Some(x, feetY, z), Some(yaw, pitch), onGround)
    case sb.PlayerLook(yaw, pitch, _onGround) =>
      updatePositionAndLook(direction = Some(yaw, pitch), onGround = _onGround)

    case packet: sb.HeldItemChange => inventory forward packet

    case playerDigging: sb.PlayerDigging =>
      (inventory ? RetrieveHeldItemId).mapTo[Option[ItemId]] onComplete {
        case Success(heldItemId) =>
          world ! PlayerDiggingHoldingItem(playerEntityId, Position(posX, posY, posZ), playerDigging, heldItemId)
        case Failure(ex) => log.error(ex, "Failed to retrieve held item.")
      }

    case packet: sb.PlayerBlockPlacement =>
      (inventory ? UseHeldItem).mapTo[Option[ItemId]] onComplete {
        case Success(Some(itemId)) => world ! PlayerPlaceBlockWithItemId(playerEntityId, packet, itemId)
        case Success(None) => // held air
        case Failure(ex) => log.error(ex, "Failed to retrieve placed block type.")
      }

    case sb.Animation(Hand.MainHand) =>
      world ! SendToAllExclude(playerEntityId, cb.Animation(playerEntityId, AnimationType.SwingMainArm))
    case sb.Animation(Hand.OffHand) =>
      world ! SendToAllExclude(playerEntityId, cb.Animation(playerEntityId, AnimationType.SwingOffHand))

    case ForwardToClient(packet) => packet match {
      case CollectItemWithType(collectItem, itemId) =>
        userContext forward collectItem
        if (collectItem.collectorEntityId == playerEntityId) {
          inventory ! AddItem(InventoryItem(itemId, collectItem.pickUpItemCount))
        }
      case _ => userContext forward packet
    }

    case packet: sb.ClickWindow if packet.windowId == PlayerInventory.Id => inventory forward packet

    case sb.EntityAction(_, _, _) => // TODO: handle this

    case RemovePlayer =>
      world ! PlayerLeavingGame(playerEntityId, username)
      reset()

    case unhandled => log.warning(s"Unhandled message in Player-$username: $unhandled")

  }

  private def playerPositionAndLookFlags(xRelative: Boolean, yRelative: Boolean, zRelative: Boolean, Y_ROT: Boolean,
                                         X_ROT: Boolean): Byte = {
    var flags: Byte = 0x00
    if (xRelative) flags = (flags | 0x01).toByte
    if (yRelative) flags = (flags | 0x02).toByte
    if (zRelative) flags = (flags | 0x04).toByte
    if (Y_ROT) flags = (flags | 0x08).toByte
    if (X_ROT) flags = (flags | 0x10).toByte

    flags
  }

  private def updatePositionAndLook(coordinates: Option[(Double, Double, Double)] = None,
                                    direction: Option[(Float, Float)] = None, onGround: Boolean = true): Unit = {

    def relative(current: Double, previous: Double): Int = ((current * 32 - previous * 32) * 128).toInt

    val relatives = coordinates map { case (x, y, z) => (relative(x, posX), relative(y, posY), relative(z, posZ)) }
    val packets = (relatives, direction) match {
      case (Some((x, y, z)), None) => List(cb.EntityRelativeMove(playerEntityId, x, y, z, onGround))
      case (None, Some((yaw, pitch))) => List(
        cb.EntityLook(playerEntityId, Angle.fromFloatYaw(yaw), Angle.fromFloatPitch(pitch), onGround),
        cb.EntityHeadLook(playerEntityId, Angle.fromFloatYaw(yaw))
      )
      case (Some((x, y, z)), Some((yaw, pitch))) => List(
        cb.EntityLookAndRelativeMove(playerEntityId, x, y, z, Angle.fromFloatYaw(yaw), Angle.fromFloatPitch(pitch), onGround),
        cb.EntityHeadLook(playerEntityId, Angle.fromFloatYaw(yaw))
      )
      case (None, None) => List(
        cb.EntityLook(playerEntityId, Angle.fromFloatYaw(yaw), Angle.fromFloatPitch(pitch), onGround)
      )
    }

    coordinates foreach { case (x, y, z) =>
      this.posX = x
      this.posY = y
      this.posZ = z
      world ! PlayerMoved(playerEntityId, Position(x, y, z))
      loadChunks()
    }

    direction foreach { case (yaw, pitch) =>
      this.yaw = yaw
      this.pitch = pitch
    }

    this.onGround = onGround

    packets foreach { packet =>
      world ! SendToAllExclude(playerEntityId, packet)
    }
  }

  private def reset(): Unit = {
    loadedChunks = Set()
    lastPosition = null
    timers.cancel(KeepAliveTickKey)
    context.become(preStartBehaviour)
  }

  private var lastPosition: (Double, Double) = _

  private def loadChunks(): Future[Unit] = {
    val timeout = 16 seconds

    def needLoadingChunks: Boolean = {
      math.abs(posX - lastPosition._1) > ServerConfiguration.LoadingChunksBlocksThreshold ||
        math.abs(posZ - lastPosition._2) > ServerConfiguration.LoadingChunksBlocksThreshold
    }

    if (lastPosition == null || needLoadingChunks) {
      val chunkX = posX.toInt >> 4
      val chunkZ = posZ.toInt >> 4
      lastPosition = (posX, posZ)
      val newChunks = (for (x <- chunkX - viewDistance to chunkX + viewDistance;
                            z <- chunkZ - viewDistance to chunkZ + viewDistance) yield (x, z)).toSet
      val toUnload = loadedChunks diff newChunks
      val toLoad = newChunks diff loadedChunks
      loadedChunks = newChunks

      toUnload foreach { case (x, z) => userContext ! UnloadChunk(x, z) }
      Future.sequence(toLoad map { case (x, z) =>
        world.ask(RequestChunkData(x, z))(timeout).mapTo[ChunkData] map { chunkData =>
          userContext ! chunkData
        }
      }) flatMap (_ => Future.unit)
    } else Future.unit
  }

  private def spawnPlayerPacket = cb.SpawnPlayer(playerEntityId, playerUUID, posX, posY, posZ,
    Angle.fromFloatYaw(yaw), Angle.fromFloatPitch(pitch), entityMetadata)

}

object Player {

  private case object KeepAliveTickKey

  private case object KeepAliveTick

  private case object KeepAliveTimeout

  private case object KeepAliveTimeoutKey

  import AttributeModifier._
  private val PlayerEntityProperties = List(
    Property(GenericMaxHealth, 20f, List()),
    Property(MovementSpeed, 0.1f, List()),
    Property(AttackSpeed, 4.0f, List()),
    Property(Armor, 0.0f, List()),
    Property(ArmorToughness, 0.0f, List()),
    Property(GenericLuck, 0.0f, List())
  )

  def props(username: String, playerUUID: UUID, serverConfiguration: ServerConfiguration): Props =
    Props(new Player(username, playerUUID: UUID, serverConfiguration))

  def name(username: String): String = s"Player-$username"

}
