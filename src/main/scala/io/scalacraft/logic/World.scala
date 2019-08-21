package io.scalacraft.logic

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Timers}
import io.scalacraft.loaders.{Blocks, Items}
import io.scalacraft.logic.World.TimeTick
import io.scalacraft.logic.messages.Message._
import io.scalacraft.misc.{Helpers, ServerConfiguration}
import io.scalacraft.packets.DataTypes.{EntityId, Position}
import io.scalacraft.packets.clientbound.PlayPackets.{BlockChange, TimeUpdate}
import io.scalacraft.packets.serverbound.PlayPackets.{Face, PlayerBlockPlacement}
import net.querz.nbt.mca.MCAUtil

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

class World(serverConfiguration: ServerConfiguration) extends Actor with ActorLogging with Timers {

  private val TimeUpdateInterval: Int = ServerConfiguration.TicksInSecond * 20
  private val TicksInDay: Int = 24000

  private var regions: Map[(Int, Int), ActorRef] = Map()
  private var players: Map[String, (UUID, ActorRef)] = Map()
  private var onlinePlayers: Map[EntityId, ActorRef] = Map()
  private val dropManager: ActorRef = context.actorOf(DropManager.props, DropManager.name)
  private val diggingManager: ActorRef = context.actorOf(DiggingManager.props(dropManager), DiggingManager.name)

  private var worldAge: Long = 0

  private val entityIdGenerator: Iterator[EntityId] = Helpers.linearCongruentialGenerator(System.nanoTime().toInt)

  override def preStart(): Unit = {
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

    case JoiningGame(playerId) => onlinePlayers += playerId -> sender

    case LeavingGame(playerId) => onlinePlayers -= playerId

    case RequestOnlinePlayers => sender ! onlinePlayers.size

    /* ----------------------------------------------- Regions ----------------------------------------------- */

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

    case request @ ChangeBlockState(Position(x, _, z), _) => regions(x >> 9, z >> 9) forward request
    case request @ RequestBlockState(Position(x, _, z)) => regions(x >> 9, z >> 9) forward request
    case request @ FindFirstSolidBlockPositionUnder(Position(x, _, z)) => regions(x >> 9, z >> 9) forward request

    case PlayerPlaceBlockWithItemId(playerId, PlayerBlockPlacement(Position(x, y, z), face, _, _, _, _), itemId) =>
      val blockState = Blocks.defaultCompoundTagFromName(Items.getStorableItemById(itemId).name)
      blockState foreach { tag => // if defined
        val position = face match {
          case Face.Bottom => Position(x, y - 1, z)
          case Face.Top => Position(x, y + 1, z)
          case Face.North => Position(x, y, z - 1)
          case Face.South => Position(x, y, z + 1)
          case Face.West => Position(x - 1, y, z)
          case Face.East => Position(x + 1, y, z)
        }

        self forward ChangeBlockState(position, tag)
        self ! SendToAllExclude(playerId, BlockChange(position, Blocks.stateIdFromCompoundTag(tag)))
      }

    /* ----------------------------------------------- Time ----------------------------------------------- */

    case TimeTick =>
      val timeOfDay = worldAge % TicksInDay
      if (worldAge % TimeUpdateInterval == 0) {
        self ! SendToAll(TimeUpdate(worldAge, timeOfDay))
      }
      worldAge += ServerConfiguration.TicksInSecond

    /* ----------------------------------------------- Misc ----------------------------------------------- */

    case RequestEntityId => sender ! entityIdGenerator.next()

    case SendToPlayer(playerId, obj) => onlinePlayers(playerId) ! ForwardToClient(obj)
    case SendToAll(obj) => onlinePlayers.values foreach (_ ! ForwardToClient(obj))
    case SendToAllExclude(excludeId, obj) => onlinePlayers foreach {
      case (playerId, ref) if playerId != excludeId => ref ! ForwardToClient(obj)
      case _ =>
    }

    /* ----------------------------------------------- Drop manager ----------------------------------------------- */

    case blockBroken: BlockBrokenAtPosition => dropManager ! blockBroken

    case message: PlayerMoved => dropManager forward message

    /* ----------------------------------------------- Digging manager ----------------------------------------------- */

    case msg: PlayerDiggingHoldingItem => diggingManager forward msg

    /* ----------------------------------------------- Default ----------------------------------------------- */

    case unhandled => log.warning(s"Unhandled message in World: $unhandled")

  }

}

object World {

  private case object TimeTick

  def props(serverConfiguration: ServerConfiguration): Props = Props(new World(serverConfiguration))

  def name: String = "world"

}
