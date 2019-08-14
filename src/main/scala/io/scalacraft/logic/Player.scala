package io.scalacraft.logic

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props, Timers}
import akka.pattern._
import io.scalacraft.logic.messages.Message.{ChunkNotPresent, JoiningGame, LeavingGame, RemovePlayer, RequestChunkData, RequestJoinGame, RequestMobsInChunk}
import io.scalacraft.logic.traits.{DefaultTimeout, ImplicitContext}
import io.scalacraft.misc.ServerConfiguration
import io.scalacraft.packets.DataTypes.Position
import io.scalacraft.packets.clientbound.PlayPackets.{ChunkData, JoinGame, SpawnMob, SpawnPosition, TimeUpdate, UnloadChunk, WorldDimension}
import io.scalacraft.packets.clientbound.{PlayPackets => cb}
import io.scalacraft.packets.serverbound.PlayPackets._
import io.scalacraft.packets.serverbound.{PlayPackets => sb}
import net.querz.nbt.mca.MCAUtil

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Random, Success}

class Player(username: String, serverConfiguration: ServerConfiguration) extends Actor
  with Timers with ActorLogging with DefaultTimeout with ImplicitContext {

  import Player._

  private val world = context.parent

  private var entityId: Int = _
  private var userContext: ActorRef = _
  private val worldDimension = WorldDimension.Overworld
  private var posX: Int = 200
  private var posY: Int = 80
  private var posZ: Int = 200
  private var yaw: Float = 0.0f
  private var pitch: Float = 0.0f
  //private var digging: Boolean = false
  private var onGround = true
  private var mainHand: MainHand = _
  private var locale: String = _
  private var viewDistance: Int = _ //render distance in chunks

  private var lastKeepAliveId: Long = _

  private var loadedChunks: Set[(Int, Int)] = Set()

  private val randomGenerator = Random

  private def preStartBehaviour: Receive = {
    case RequestJoinGame(playerEntityId, playerUserContext) =>
      entityId = playerEntityId
      userContext = playerUserContext

      userContext ! JoinGame(entityId, serverConfiguration.gameMode, worldDimension,
        serverConfiguration.serverDifficulty, serverConfiguration.maxPlayers, serverConfiguration.levelTypeBiome,
        serverConfiguration.reducedDebugInfo)
      world ! JoiningGame
      // userContext ! cb.PluginMessage("minecraft:brand", Array(7, 118, 97, 110, 105, 108, 108, 97))
      // userContext ! cb.PlayerAbilities(0)

    case clientSettings: ClientSettings =>
      locale = clientSettings.locale
      mainHand = clientSettings.mainHand
      viewDistance = if (clientSettings.viewDistance > ServerConfiguration.MaxViewDistance)
        ServerConfiguration.MaxViewDistance else clientSettings.viewDistance

      loadChunks() onComplete {
        case Success(_) =>
          userContext ! SpawnPosition(Position(posX, posY, posZ))

          val teleportId = randomGenerator.nextInt()
          val flags = playerPositionAndLookFlags(xRelative = false, yRelative = false, zRelative = false, Y_ROT = false, X_ROT = false)
          val positionAndLook = cb.PlayerPositionAndLook(posX, posY, posZ, yaw, pitch, flags, teleportId)
          userContext ! positionAndLook

          context.become(confirmTeleport(positionAndLook))
        case Failure(e) => log.error(e, "Cannot load chunks")
      }
    case clientStatus: ClientStatus if clientStatus.action == ClientStatusAction.PerformRespawn =>
      log.warning("Respawn not to be handled on login")
    case clientStatus: ClientStatus if clientStatus.action == ClientStatusAction.RequestStats =>
      log.warning("User request statistics, not handled")
  }

  private def confirmTeleport(positionAndLook: cb.PlayerPositionAndLook): Receive = {
    case teleportConfirm: TeleportConfirm =>
      if (teleportConfirm.teleportId == positionAndLook.teleportId) {
        userContext ! positionAndLook

        timers.startPeriodicTimer(KeepAliveTickKey, KeepAliveTick, 5 seconds)
        context.become(playingBehaviour)
      } else {
        log.warning("Client not confirm teleport")
      }
  }

  private def playingBehaviour: Receive = {
    case KeepAliveTick =>
      lastKeepAliveId = randomGenerator.nextLong()
      userContext ! cb.KeepAlive(lastKeepAliveId)
      if(!timers.isTimerActive(KeepAliveTimeoutKey)) {
        timers.startSingleTimer(KeepAliveTimeoutKey, KeepAliveTimeout, 30.seconds)
      }
    case KeepAliveTimeout =>
      timers.cancel(KeepAliveTickKey)
    case keepAlive: sb.KeepAlive if keepAlive.keepAliveId == lastKeepAliveId =>
      timers.cancel(KeepAliveTimeoutKey)
    case keepAlive: sb.KeepAlive if keepAlive.keepAliveId != lastKeepAliveId =>

    case player: Player =>
      onGround = player.onGround
    case playerPosition: PlayerPosition =>
      posX = playerPosition.x.toInt
      posY = playerPosition.feetY.toInt
      posZ = playerPosition.z.toInt
      onGround = playerPosition.onGround
      loadChunks()
    case playerPositionAndLook: sb.PlayerPositionAndLook =>
      posX = playerPositionAndLook.x.toInt
      posY = playerPositionAndLook.feetY.toInt
      posZ = playerPositionAndLook.z.toInt
      yaw = playerPositionAndLook.yaw
      pitch =playerPositionAndLook.pitch
      onGround = playerPositionAndLook.onGround
      loadChunks()
    case timeUpdate: TimeUpdate => userContext forward timeUpdate
    case RemovePlayer =>
      world ! LeavingGame
      reset()
    case spawnMobs: List[SpawnMob] => spawnMobs.foreach(spawnMob => userContext ! spawnMob)
    case _: Animation=>
      world ! RequestMobsInChunk(posX, posZ)
  }

  override def receive: Receive = preStartBehaviour

  private def playerPositionAndLookFlags(xRelative: Boolean, yRelative: Boolean, zRelative: Boolean, Y_ROT: Boolean, X_ROT: Boolean): Byte = {
    var flags: Byte = 0x00
    if (xRelative) flags = (flags | 0x01).toByte
    if (yRelative) flags = (flags | 0x02).toByte
    if (zRelative) flags = (flags |  0x04).toByte
    if (Y_ROT) flags = (flags |  0x08).toByte
    if (X_ROT) flags = (flags |  0x10).toByte
    flags
  }

  private def reset(): Unit = {
    loadedChunks = Set()
    lastPosition = null
    context.become(preStartBehaviour)
  }

  private var lastPosition: (Int, Int) = _
  private def loadChunks(): Future[Unit] = {
    val timeout = 16 seconds
    def needLoadingChunks: Boolean = {
      math.abs(posX - lastPosition._1) > ServerConfiguration.LoadingChunksBlocksThreshold ||
        math.abs(posZ - lastPosition._2) > ServerConfiguration.LoadingChunksBlocksThreshold
    }

    if (lastPosition == null || needLoadingChunks) {
      val chunkX = posX >> 4
      val chunkZ = posZ >> 4
      lastPosition = (posX, posZ)
      val newChunks = (for (x <- chunkX - viewDistance until chunkX + viewDistance;
           z <- chunkZ - viewDistance until chunkZ + viewDistance) yield (x, z)).toSet
      val toUnload = loadedChunks diff newChunks
      val toLoad = newChunks diff loadedChunks
      loadedChunks = newChunks

      toUnload foreach { case (x, z) => userContext ! UnloadChunk(x, z) }
      Future.sequence(toLoad map { case (x, z) =>
        world.ask(RequestChunkData(x, z))(timeout) map {
          case chunkData: ChunkData => userContext ! chunkData
          case _ => // do nothing
        }
      }) map (_ => Unit)
    } else Future.unit
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
