package io.scalacraft.logic

import java.nio.charset.Charset
import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Timers}
import akka.pattern._
import io.scalacraft.loaders.{Blocks, Items}
import io.scalacraft.logic.commons.Message._
import io.scalacraft.logic.commons.{DefaultTimeout, ImplicitContext}
import io.scalacraft.misc.{Helpers, ServerConfiguration}
import io.scalacraft.packets.DataTypes.{EntityId, Position}
import io.scalacraft.packets.clientbound.PlayPackets._
import io.scalacraft.packets.serverbound.PlayPackets.{Face, PlayerBlockPlacement}
import net.querz.nbt.mca.MCAUtil

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}


class World(serverConfiguration: ServerConfiguration) extends Actor
  with ActorLogging with Timers with ImplicitContext with DefaultTimeout {

  import World._

  private val TimeUpdateInterval: Int = ServerConfiguration.TicksInSecond * 20
  private val TicksInDay: Int = 24000

  private var regions: Map[(Int, Int), ActorRef] = Map()
  private var players: Map[String, ActorRef] = Map()
  private var onlinePlayers: Map[EntityId, (String, ActorRef)] = Map()
  private val dropManager: ActorRef = context.actorOf(DropManager.props, DropManager.name)
  private val diggingManager: ActorRef = context.actorOf(DiggingManager.props(dropManager), DiggingManager.name)
  private var creatureSpawner: ActorRef = _
  private var creatureRemovalsPending: List[Int] = List()

  private var worldAge: Long = 0

  private val entityIdGenerator: Iterator[EntityId] = Helpers.linearCongruentialGenerator(System.nanoTime().toInt)

  override def preStart(): Unit = {
    import World._
    creatureSpawner = context.actorOf(CreatureSpawner.props, CreatureSpawner.name)
    timers.startPeriodicTimer(new Object(), TimeTick, 1 second)
  }

  override def receive: Receive = {

    /* ----------------------------------------------- Players ----------------------------------------------- */

    case RegisterUser(username) =>
      if (!players.contains(username)) {
        players += username ->
          context.actorOf(Player.props(username, playerUUID(username), serverConfiguration), Player.name(username))
      }

      sender ! UserRegistered(entityIdGenerator.next(), playerUUID(username), players(username))

    case PlayerJoiningGame(playerId, username) =>
      val addPlayer = PlayerInfoAddPlayer(playerUUID(username), username, List.empty,
        serverConfiguration.gameMode.value, ping = 0, displayName = None)
      onlinePlayers += playerId -> (username, sender)
      self ! SendToAllExclude(playerId, PlayerInfo(List(addPlayer)))
      self ! SendToAll(ChatMessage(chatMessageJson(username, playerUUID(username)), ChatPosition.SystemMessage))

    case PlayerLeavingGame(playerId, username) =>
      onlinePlayers -= playerId
      self ! SendToAll(PlayerInfo(List(PlayerInfoRemovePlayer(playerUUID(username)))))
      self ! SendToAll(DestroyEntities(List(playerId)))

    case PlayerSpawning(playerId, spawnPacket, entityProperties) =>
      self ! SendToAllExclude(playerId, spawnPacket)
      self ! SendToAllExclude(playerId, entityProperties)

      val addPlayers = onlinePlayers map {
        case (_, (username, _)) => PlayerInfoAddPlayer(playerUUID(username), username, List.empty,
          serverConfiguration.gameMode.value, ping = 0, displayName = None)
      }
      self ! SendToPlayer(playerId, PlayerInfo(addPlayers.toList))

      onlinePlayers foreach {
        case (id, (_, ref)) if id != playerId => (ref ? RequestSpawnPacket) onComplete {
          case Success(packet) => self ! SendToPlayer(playerId, packet)
          case Failure(ex) => log.error(ex, "Unable to perform RequestSpawnPacket in PlayerSpawning")
        }
        case _ =>
      }

    case RequestOnlinePlayers => sender ! onlinePlayers.size

    /* ----------------------------------------------- Regions ----------------------------------------------- */

    case request@RequestChunkData(chunkX, chunkZ, _) => forwardToRegionWithChunk(chunkX, chunkZ)(request)
    case action@ChangeBlockState(Position(blockX, _, blockZ), _) => forwardToRegionWithBlock(blockX, blockZ)(action)
    case request@RequestBlockState(Position(blockX, _, blockZ)) => forwardToRegionWithBlock(blockX, blockZ)(request)
    case request@FindFirstSolidBlockPositionUnder(Position(blockX, _, blockZ)) =>
      forwardToRegionWithBlock(blockX, blockZ)(request)

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
        self ! SendToAll(DestroyEntities(creatureRemovalsPending))
        creatureRemovalsPending = List()
      }
      creatureSpawner ! SkyStateUpdate(SkyUpdateState.timeUpdateStateFromTime(timeOfDay))
      worldAge += ServerConfiguration.TicksInSecond

    /* ----------------------------------------------- General ----------------------------------------------- */

    case RequestEntityId => sender ! entityIdGenerator.next()

    case SendToPlayer(playerId, obj) => onlinePlayers(playerId)._2 ! ForwardToClient(obj)
    case SendToAll(obj) => onlinePlayers.values foreach { case (_, ref) => ref ! ForwardToClient(obj) }
    case SendToAllExclude(excludeId, obj) => onlinePlayers foreach {
      case (playerId, (_, ref)) if playerId != excludeId => ref ! ForwardToClient(obj)
      case _ =>
    }

    /* ----------------------------------------------- Drop manager ----------------------------------------------- */

    case itemDropped: DropItems => dropManager ! itemDropped

    case blockBroken: BlockBrokenAtPosition => dropManager ! blockBroken

    case message: PlayerMoved => dropManager forward message

    /* --------------------------------------------- Digging manager --------------------------------------------- */

    case msg: PlayerDiggingHoldingItem => diggingManager forward msg

    /* ----------------------------------------------- Mobs ----------------------------------------------- */

    case request: SpawnCreaturesInChunk => creatureSpawner forward request
    case request @ RequestSpawnPoints(chunkX, chunkZ) => forwardToRegionWithChunk(chunkX, chunkZ)(request)
    case action: PlayerUnloadedChunk => creatureSpawner forward action
    case request @ RequestNearbyPoints(blockX,_, blockZ, _, _) => forwardToRegionWithBlock(blockX, blockZ)(request)
    case useEntityWithItem: UseEntityWithItem => creatureSpawner forward useEntityWithItem
    case EntityDead(entityId) => creatureRemovalsPending +:= entityId

    /* ----------------------------------------------- Default ----------------------------------------------- */

    case unhandled => log.warning(s"Unhandled message in World: $unhandled")

  }

  private def forwardToRegionWithBlock(blockX: Int, blockZ: Int)(message: Any): Unit =
    forwardToRegionWithChunk(blockX >> 4, blockZ >> 4)(message)

  private def forwardToRegionWithChunk(chunkX: Int, chunkZ: Int)(message: Any): Unit = {
    val (x, z) = (chunkX >> 5, chunkZ >> 5)
    if (regions.contains((x, z))) {
      regions((x, z)) forward message
    } else {
      Try(MCAUtil.readMCAFile(s"world/regions/r.$x.$z.mca")) match {
        case Success(regionFile) =>
          val region = context.actorOf(Region.props(regionFile), Region.name(x, z))
          regions += (x, z) -> region
          region forward message
        case Failure(_) =>
          log.warning(s"Region ($x,$z) not present")
          sender ! ChunkNotPresent
      }
    }
  }

  private def playerUUID(username: String): UUID =
    UUID.nameUUIDFromBytes(s"OfflinePlayer:$username".getBytes(Charset.forName("UTF-8")))

}

object World {

  private case object TimeTick

  private def chatMessageJson(username: String, uuid: UUID) = s"""{"color":"yellow","translate":"multiplayer.player.joined","with":[{"insertion":"$username","clickEvent":{"action":"suggest_command","value":"/tell $username "},"text":"$username"}]}"""

  def props(serverConfiguration: ServerConfiguration): Props = Props(new World(serverConfiguration))

  def name: String = "world"

}
