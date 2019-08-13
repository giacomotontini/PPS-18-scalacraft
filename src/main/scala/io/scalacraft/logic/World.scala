package io.scalacraft.logic

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Timers}
import com.typesafe.scalalogging.LazyLogging
import io.scalacraft.loaders.Regions
import io.scalacraft.logic.DiggingManager.Message.PlayerDiggingWithItem
import io.scalacraft.logic.World.TimeTick
import io.scalacraft.logic.messages.Message._
import io.scalacraft.misc.ServerConfiguration
import io.scalacraft.packets.clientbound.PlayPackets._
import net.querz.nbt.mca.MCAUtil

import scala.concurrent.duration._
import scala.language.postfixOps

class World(serverConfiguration: ServerConfiguration) extends Actor with LazyLogging with ActorLogging with Timers {

  private val TimeUpdateInterval: Int = ServerConfiguration.TicksInSecond * 20
  private val TicksInDay: Int = 24000

  private var regions: Map[(Int, Int), ActorRef] = _
  private var players: Map[String, UUID] = Map()
  private var onlinePlayers: List[ActorRef] = List()
  private var diggingManager: ActorRef = _

  private var worldAge: Long = 0

  override def preStart(): Unit = {
    import World._

    try {
      log.debug("Loading regions..")
      regions = Regions.loadRegions() map {
        case ((x, y), file) => (x, y) -> context.actorOf(Region.props(file), Region.name(x, y))
      }
      diggingManager = context.actorOf(DiggingManager.props(self))
    } catch {
      case e: Exception => logger.error("Error loading the world", e)
    }

    timers.startPeriodicTimer(new Object(), TimeTick, 1 second)
  }

  override def receive: Receive = {
    /* ----------------------------------------------- Users ----------------------------------------------- */
    case RegisterUser(username: String, userContext: ActorRef) =>
      val uuid = if (players.contains(username)) players(username)
      else {
        val newUUID = UUID.randomUUID()
        players += username -> newUUID
        newUUID
      }
      val player = context.actorOf(Player.props(username, userContext, serverConfiguration), Player.name(username))
      sender ! UserRegistered(uuid, player)
    case JoiningGame => onlinePlayers +:= sender
    case LeavingGame => onlinePlayers = onlinePlayers filter {
      _ != sender
    }
    case RequestOnlinePlayers => sender ! OnlinePlayers(onlinePlayers.size)
    case request@RequestChunkData(chunkX, chunkZ, _) =>
      val (relativeX, relativeZ) = (MCAUtil.chunkToRegion(chunkX), MCAUtil.chunkToRegion(chunkZ))
      if (regions.contains((relativeX, relativeZ))) {
        regions((relativeX, relativeZ)) forward request
      } else {
        log.warning(s"Region ($relativeX,$relativeZ) not loaded")
        sender ! ChunkNotPresent
      }
    case TimeTick =>
      val timeOfDay = worldAge % TicksInDay

      if (worldAge % TimeUpdateInterval == 0) {
        val timeUpdate = TimeUpdate(worldAge, timeOfDay)
        onlinePlayers foreach {
          _ ! timeUpdate
        }
      }

      worldAge += ServerConfiguration.TicksInSecond
    case request@BlockBreakAtPosition(position, playerId) =>
      val (relativeX, relativeZ) = (MCAUtil.blockToRegion(position.x), MCAUtil.blockToRegion(position.z))
      regions(relativeX, relativeZ) forward (request)
      sendToPlayers(Effect(EffectId.BlockBreakWithSound, position, 0, disableRelativeVolume = false))
      sendToPlayers(BlockBreakAnimation(playerId, position, 10))
      sendToPlayers(BlockChange(position, 0))
    case ForwardToWorld(message, playerId) => message match {
      case _ =>
      //case sb.Animation(hand) if hand == Hand.MainHand => sendToPlayers(cb.Animation(playerId, AnimationType.SwingMainArm))
      //case sb.Animation(hand) if hand == Hand.OffHand => sendToPlayers(cb.Animation(playerId, AnimationType.SwingOffHand))
    }
    case msg: PlayerDiggingWithItem => diggingManager forward msg
    case msg => sendToPlayers(msg)
  }

  private def sendToPlayers(obj: Any): Unit = {
    context.actorSelection("/user/world/Player*") ! ForwardToClient(obj)
  }

  private def sentToPlayer(username: String, obj: Any): Unit = {
    context.actorSelection("/user/world/Player-" + username) ! ForwardToClient(obj)
  }

}

object World {

  private case object TimeTick

  def props(serverConfiguration: ServerConfiguration): Props = Props(new World(serverConfiguration))

  def name: String = "world"

}
