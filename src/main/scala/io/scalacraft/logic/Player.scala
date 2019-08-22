package io.scalacraft.logic

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Stash, Timers}
import akka.pattern._
import io.scalacraft.logic.PlayerInventoryActor.Message.{AddItem, RetrieveHeldItemId, UseHeldItem}
import io.scalacraft.logic.messages.Message._
import io.scalacraft.logic.traits.{DefaultTimeout, ImplicitContext}
import io.scalacraft.misc.ServerConfiguration
import io.scalacraft.packets.DataTypes.{ItemId, Position}
import io.scalacraft.packets.clientbound.PlayPackets._
import io.scalacraft.packets.clientbound.{PlayPackets => cb}
import io.scalacraft.packets.serverbound.PlayPackets._
import io.scalacraft.packets.serverbound.{PlayPackets => sb}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Random, Success}

class Player(username: String, serverConfiguration: ServerConfiguration) extends Actor
  with Timers with ActorLogging with DefaultTimeout with ImplicitContext with Stash {

  import Player._

  private val world = context.parent
  private val inventory = context.actorOf(PlayerInventoryActor.props(self))
  private var userContext: ActorRef = _

  private var playerEntityId = 0
  private val worldDimension = WorldDimension.Overworld
  private var posX: Int = 583
  private var posY: Int = 80
  private var posZ: Int = -361
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

  private var loadedChunks: Set[(Int, Int)] = Set()

  private val randomGenerator = Random

  override def receive: Receive = preStartBehaviour

  private def preStartBehaviour: Receive = {
    case RequestJoinGame(playerEntityId, playerUserContext) =>
      this.playerEntityId = playerEntityId
      userContext = playerUserContext

      userContext ! JoinGame(playerEntityId, serverConfiguration.gameMode, worldDimension,
        serverConfiguration.serverDifficulty, serverConfiguration.maxPlayers, serverConfiguration.levelTypeBiome,
        serverConfiguration.reducedDebugInfo)
      world ! JoiningGame(playerEntityId)
      // userContext ! cb.PluginMessage("minecraft:brand", Array(7, 118, 97, 110, 105, 108, 108, 97))
      // userContext ! cb.PlayerAbilities(0)

    case clientSettings: sb.ClientSettings =>
      locale = clientSettings.locale
      mainHand = clientSettings.mainHand
      viewDistance = if (clientSettings.viewDistance > ServerConfiguration.MaxViewDistance)
        ServerConfiguration.MaxViewDistance else clientSettings.viewDistance

      loadChunksAndMobs() onComplete {
        case Success(_) =>
          userContext ! SpawnPosition(Position(posX, posY, posZ))

          val teleportId = randomGenerator.nextInt()
          val flags = playerPositionAndLookFlags(xRelative = false, yRelative = false, zRelative = false, Y_ROT = false, X_ROT = false)
          val positionAndLook = cb.PlayerPositionAndLook(posX, posY, posZ, yaw, pitch, flags, teleportId)
          userContext ! positionAndLook

          context.become(confirmTeleport(positionAndLook))
        case Failure(e) => log.error(e, "Cannot load chunks.")
      }

    case ClientStatus(ClientStatusAction.PerformRespawn) => log.warning("Respawn not to be handled on login.")
    case ClientStatus(ClientStatusAction.RequestStats) => log.warning("User request statistics, not handled.")
  }

  private def confirmTeleport(positionAndLook: cb.PlayerPositionAndLook): Receive = {
    case teleportConfirm: sb.TeleportConfirm =>
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

    case KeepAliveTimeout => timers.cancel(KeepAliveTickKey)
    case sb.KeepAlive(keepAliveId) if keepAliveId == lastKeepAliveId => timers.cancel(KeepAliveTimeoutKey)
    case sb.KeepAlive(keepAliveId) if keepAliveId != lastKeepAliveId => log.warning("Client keep alive id invalid.")

    case sb.Player(onGround) => this.onGround = onGround

    case sb.PlayerPosition(x, feetY, z, onGround) =>
      posX = x.toInt
      posY = feetY.toInt
      posZ = z.toInt
      this.onGround = onGround
      world ! PlayerMoved(playerEntityId, Position(posX, posY, posZ))
      loadChunksAndMobs()

    case sb.PlayerPositionAndLook(x, feetY, z, yaw, pitch, onGround) =>
      posX = x.toInt
      posY = feetY.toInt
      posZ = z.toInt
      this.yaw = yaw
      this.pitch = pitch
      this.onGround = onGround
      world ! PlayerMoved(playerEntityId, Position(posX, posY, posZ))
      loadChunksAndMobs()

    case sb.PlayerLook(yaw, pitch, onGround) =>
      this.yaw = yaw
      this.pitch = pitch
      this.onGround = onGround

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

    case sb.ChatMessage(message) => handleDebugCommand(message)

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
      world ! LeavingGame(playerEntityId)
      reset()

    case entityRelativeMove: EntityRelativeMove => userContext ! entityRelativeMove
    case entityLookAndRelativeMove: EntityLockAndRelativeMove => userContext ! entityLookAndRelativeMove
    case entityVelocity: EntityVelocity => userContext ! entityVelocity
    case entityLook: EntityLook => userContext ! entityLook
    case entityHeadLook: EntityHeadLook =>
     userContext ! entityHeadLook
    /*case spawnMobs: List[SpawnMob] => spawnMobs.foreach(spawnMob => userContext ! spawnMob)
    case destroyCreautures: List[DestroyEntities] => destroyCreautures.foreach(destroyCreaure => userContext ! destroyCreaure)
    case _: sb.Animation=>
      world ! Height(posX, posY,posZ)
    case pos: List[Some[Position]] => println(pos)*/
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

  private def reset(): Unit = {
    loadedChunks = Set()
    lastPosition = null
    context.become(preStartBehaviour)
  }

  private var lastPosition: (Int, Int) = _
  private def loadChunksAndMobs(): Future[Unit] = {
    val timeout = 30 seconds
    def needLoadingChunks: Boolean = {
      math.abs(posX - lastPosition._1) > ServerConfiguration.LoadingChunksBlocksThreshold ||
        math.abs(posZ - lastPosition._2) > ServerConfiguration.LoadingChunksBlocksThreshold
    }

    def loadChunks(toUnload: Set[(Int, Int)], toLoad: Set[(Int, Int)]): Future[Unit] = {
      toUnload foreach { case (x, z) => userContext ! UnloadChunk(x, z) }
      Future.sequence(toLoad map { case (x, z) =>
        world.ask(RequestChunkData(x, z))(timeout) map {
          case chunkData: ChunkData =>
            userContext ! chunkData
          case _ => // do nothing
        }
      }) map (_ => Unit)
    }
    def loadMobs(toUnload: Set[(Int, Int)], toLoad: Set[(Int, Int)]): Future[Unit] = {
      val unloadFuture = Future.sequence(toUnload map { case (x, z) =>
        world.ask(PlayerUnloadedChunk(x, z))(timeout).mapTo[List[DestroyEntities]] map (destroyCreatures =>
          destroyCreatures.foreach(destroyCreaure => userContext ! destroyCreaure))
      }) map (_ => Unit)
      val loadFuture = Future.sequence(toLoad map { case (x, z) =>
        world.ask(SpawnCreaturesInChunk(x, z))(timeout).mapTo[List[SpawnMob]] map (spawnMobs =>
          spawnMobs.foreach(spawnMob => userContext ! spawnMob))
      }) map (_ => Unit)
      loadFuture.zip(unloadFuture).flatMap(_ => Future.unit)
    }

    if (lastPosition == null || needLoadingChunks) {
      val chunkX = posX >> 4
      val chunkZ =  posZ >> 4
      lastPosition = (posX, posZ)
      val newChunks = (for (x <- chunkX - viewDistance to chunkX + viewDistance;
           z <- chunkZ - viewDistance to chunkZ + viewDistance) yield (x, z)).toSet
      val toUnload = loadedChunks diff newChunks
      val toLoad = newChunks diff loadedChunks
      loadedChunks = newChunks

      val loadChunksFuture = loadChunks(toUnload, toLoad)
      val loadMobFuture = loadMobs(toUnload, toLoad)
      loadChunksFuture.zip(loadMobFuture).flatMap(_ => Future.unit)
    } else Future.unit
  }

  implicit class Regex(sc: StringContext) {
    def r = new util.matching.Regex(sc.parts.mkString, sc.parts.tail.map(_ => "x"): _*)
  }

  private def handleDebugCommand(message: String): Unit = message match {
    case r"/sb (-?\d+)$x (-?\d+)$y (-?\d+)$z" => println(x, y, z)
    case _ => println("nope")
  }

}

object Player {

  private case object KeepAliveTickKey

  private case object KeepAliveTick

  private case object KeepAliveTimeout

  private case object KeepAliveTimeoutKey

  def props(username: String, serverConfiguration: ServerConfiguration): Props =
    Props(new Player(username, serverConfiguration))

  def name(username: String): String = s"Player-$username"

}
