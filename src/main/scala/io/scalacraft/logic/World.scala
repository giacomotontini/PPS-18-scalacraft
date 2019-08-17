package io.scalacraft.logic

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Timers}
import com.typesafe.scalalogging.LazyLogging
import io.scalacraft.logic.DiggingManager.Message.PlayerDiggingWithItem
import io.scalacraft.logic.World.TimeTick
import io.scalacraft.logic.messages.Message._
import io.scalacraft.misc.{Helpers, ServerConfiguration}
import io.scalacraft.packets.clientbound.PlayPackets.{TimeUpdate, _}
import io.scalacraft.packets.clientbound.{PlayPackets => cb}
import io.scalacraft.packets.serverbound.PlayPackets._
import io.scalacraft.packets.serverbound.{PlayPackets => sb}
import net.querz.nbt.mca.MCAUtil

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

class World(serverConfiguration: ServerConfiguration) extends Actor with LazyLogging with ActorLogging with Timers {

  private val TimeUpdateInterval: Int = ServerConfiguration.TicksInSecond * 20
  private val TicksInDay: Int = 24000

  private var regions: Map[(Int, Int), ActorRef] = Map()
  private var players: Map[String, (UUID, ActorRef)] = Map()
  implicit private var onlinePlayers: List[ActorRef] = List()
  private var diggingManager: ActorRef = _

  private var worldAge: Long = 0

  private val entityIdGenerator: Iterator[Int] = Helpers.linearCongruentialGenerator(System.nanoTime().toInt)

  override def preStart(): Unit = {
    diggingManager = context.actorOf(DiggingManager.props(self))
    timers.startPeriodicTimer(new Object(), TimeTick, 1 second)
  }

  override def receive: Receive = {
    /* ----------------------------------------------- Users ----------------------------------------------- */
    case RegisterUser(username) =>
      val (uuid, player) = if (players.contains(username)) players(username)
      else {
        val newUUID = UUID.randomUUID()
        val actorRef = context.actorOf(Player.props(username, serverConfiguration), Player.name(username))
        players += username -> (newUUID, actorRef)
        (newUUID, actorRef)
      }

      sender ! UserRegistered(entityIdGenerator.next(), uuid, player)
    case JoiningGame => onlinePlayers +:= sender
    case LeavingGame => onlinePlayers = onlinePlayers filter {_ != sender}
    case RequestOnlinePlayers => sender ! onlinePlayers.size
    case request @ RequestChunkData(chunkX, chunkZ, _) =>
      val (relativeX, relativeZ) = (MCAUtil.chunkToRegion(chunkX), MCAUtil.chunkToRegion(chunkZ))
      if (regions.contains((relativeX, relativeZ))) {
        regions((relativeX, relativeZ)) forward request
      } else {
        Try(MCAUtil.readMCAFile(s"world/regions/r.$relativeX.$relativeZ.mca")) match {
          case Success(regionFile) =>
            val region = context.actorOf(Region.props(regionFile), Region.name(relativeX, relativeZ))
            regions += (relativeX, relativeZ) -> region
            region forward request
          case Failure(_) =>
            log.warning(s"Region ($relativeX,$relativeZ) not loaded")
            sender ! ChunkNotPresent
        }
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
    case RequestEntityId => sender ! entityIdGenerator.next()
    case request@BlockBreakAtPosition(position, playerId) =>
      val (relativeX, relativeZ) = (MCAUtil.blockToRegion(position.x), MCAUtil.blockToRegion(position.z))
      regions(relativeX, relativeZ) forward (request)
      sendToPlayers(Effect(EffectId.BlockBreakWithSound, position, 0, disableRelativeVolume = false))
      sendToPlayers(BlockBreakAnimation(playerId, position, 10))
      sendToPlayers(BlockChange(position, 0))
    case PlayerAnimation(username, playerId, animation: sb.Animation) => animation.hand match {
      case Hand.MainHand => sendToPlayersExceptOne(username, cb.Animation(playerId, AnimationType.SwingMainArm))
      case Hand.OffHand => sendToPlayersExceptOne(username, cb.Animation(playerId, AnimationType.SwingOffHand))
    }
    case msg @ BlockPlacedByUser(playerBlockPlacement, _,_) => {
      val (relativeX, relativeZ) = (MCAUtil.blockToRegion(playerBlockPlacement.position.x), MCAUtil.blockToRegion(playerBlockPlacement.position.z))
      regions(relativeX, relativeZ) forward msg
    }
    case msg: PlayerDiggingWithItem => diggingManager forward msg
    case msg => sendToPlayers(msg)
  }

  private def sendToPlayersExceptOne(username: String, obj: Any): Unit = {
    sendToPlayers(obj)(onlinePlayers.filter(!_.path.name.contains("Player-" + username)))
  }

  private def sendToPlayers(obj: Any)(implicit players :List[ActorRef]): Unit = {
    players foreach (_ ! ForwardToClient(obj))
  }

  private def sendToPlayer(username: String, obj: Any): Unit = {
    onlinePlayers.foreach {
      case actor:ActorRef if actor.path.name.contains("Player-" + username) => actor ! ForwardToClient(obj)
    }
  }

}

object World {

  private case object TimeTick

  def props(serverConfiguration: ServerConfiguration): Props = Props(new World(serverConfiguration))

  def name: String = "world"

}
