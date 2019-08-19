package io.scalacraft.logic

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Timers}
import com.typesafe.scalalogging.LazyLogging
import io.scalacraft.logic.World.TimeTick
import io.scalacraft.logic.messages.Message._
import io.scalacraft.misc.{Helpers, ServerConfiguration}
import io.scalacraft.packets.clientbound.PlayPackets.{EntityHeadLook, EntityLockAndRelativeMove, EntityLook, EntityRelativeMove, EntityVelocity, TimeUpdate}
import net.querz.nbt.mca.MCAUtil

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

class World(serverConfiguration: ServerConfiguration) extends Actor with LazyLogging with ActorLogging with Timers {

  private val TimeUpdateInterval: Int = ServerConfiguration.TicksInSecond * 20
  private val TicksInDay: Int = 24000

  private var regions: Map[(Int, Int), ActorRef] = Map()
  private var players: Map[String, (UUID, ActorRef)] = Map()
  private var creatureSpawner: ActorRef = _
  private var onlinePlayers: List[ActorRef] = List()

  private var worldAge: Long = 0

  private val entityIdGenerator: Iterator[Int] = Helpers.linearCongruentialGenerator(System.nanoTime().toInt)

  override def preStart(): Unit = {
    import World._
    creatureSpawner = context.actorOf(CreatureSpawner.props, CreatureSpawner.name)
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
        onlinePlayers foreach { _ ! timeUpdate}
      }
      creatureSpawner ! SkyStateUpdate(SkyUpdateState.timeUpdateStateFromTime(timeOfDay))
      worldAge += ServerConfiguration.TicksInSecond
    case RequestEntityId => sender ! entityIdGenerator.next()
    case requestMobs: RequestMobsInChunk =>
      creatureSpawner forward requestMobs
    case requestSpawnPoints @ RequestSpawnPoints(chunkX, chunkZ) => regions(MCAUtil.chunkToRegion(chunkX), MCAUtil.chunkToRegion(chunkZ)) forward requestSpawnPoints
    case unloadedChunk: PlayerUnloadedChunk =>
      creatureSpawner forward unloadedChunk
    case entityRelativeMove: EntityRelativeMove =>
      players.foreach(player => player._2._2 forward entityRelativeMove)
    case entityLookAndRelativeMove: EntityLockAndRelativeMove =>
      players.foreach(player => player._2._2 forward entityLookAndRelativeMove)
    case entityVelocity: EntityVelocity =>
      players.foreach(player => player._2._2 forward entityVelocity)
    case requestNearbyPoints @ RequestNearbyPoints(posX,_, posZ, _,_,_) => regions(MCAUtil.blockToRegion(posX), MCAUtil.blockToRegion(posZ)) forward requestNearbyPoints
    case entityLook: EntityLook =>
      players.foreach(player => player._2._2 forward entityLook)
    case entityHeadLook: EntityHeadLook =>
      players.foreach(player => player._2._2 forward entityHeadLook)
    case height @ Height(x,_,z) => regions(MCAUtil.blockToRegion(x), MCAUtil.blockToRegion(z)) forward height
  }

}

object World {

  private case object TimeTick

  def props(serverConfiguration: ServerConfiguration): Props = Props(new World(serverConfiguration))
  def name: String = "world"

}
