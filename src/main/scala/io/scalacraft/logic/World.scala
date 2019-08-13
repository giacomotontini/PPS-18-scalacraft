package io.scalacraft.logic

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.typesafe.scalalogging.LazyLogging
import io.scalacraft.loaders.Regions
import io.scalacraft.logic.DiggingManager.Message.PlayerDiggingWithItem
import io.scalacraft.logic.messages.Message.{BlockBreakAtPosition, ChunkNotPresent, ForwardToClient, ForwardToWorld, RegisterUser, RequestChunkData, UserRegistered}
import io.scalacraft.packets.clientbound.PlayPackets._
import io.scalacraft.packets.clientbound.{PlayPackets => cb}
import io.scalacraft.packets.serverbound.PlayPackets._
import io.scalacraft.packets.serverbound.{PlayPackets => sb}
import net.querz.nbt.mca.MCAUtil

class World extends Actor with LazyLogging with ActorLogging {

  private var regions: Map[(Int, Int), ActorRef] = _
  private var players: Map[String, UUID] = Map()
  private var diggingManager: ActorRef = _

  override def preStart(): Unit = {
    try {
      log.debug("Loading regions..")
      regions = Regions.loadRegions() map {
        case ((x, y), file) => (x, y) -> context.actorOf(Region.props(file), Region.name(x, y))
      }
      diggingManager = context.actorOf(DiggingManager.props(self))
    } catch {
      case e: Exception => logger.error("Error loading the world", e)
    }
  }

  override def receive: Receive = {
    case RegisterUser(username: String, userContext: ActorRef) =>
      val uuid = if (players.contains(username)) players(username)
      else {
        val newUUID = UUID.randomUUID()
        players += username -> newUUID
        newUUID
      }
      val player = context.actorOf(Player.props(username, userContext), Player.name(username))
      sender ! UserRegistered(uuid, player)
    case request @ RequestChunkData(chunkX, chunkZ, _) =>
      val (relativeX, relativeZ) = (MCAUtil.chunkToRegion(chunkX), MCAUtil.chunkToRegion(chunkZ))
      if (regions.contains((relativeX, relativeZ))) {
        regions((relativeX, relativeZ)) forward request
      } else {
        log.warning(s"Region ($relativeX,$relativeZ) not loaded")
        sender ! ChunkNotPresent
      }
    case request @ BlockBreakAtPosition(position, playerId) =>
      val (relativeX, relativeZ) = (MCAUtil.blockToRegion(position.x), MCAUtil.blockToRegion(position.z))
      regions(relativeX, relativeZ) forward(request)
      sendToPlayers(Effect(EffectId.BlockBreakWithSound, position , 0, false))
      sendToPlayers(BlockBreakAnimation(playerId, position , 10))
      sendToPlayers(BlockChange(position, 0))
    case ForwardToWorld(message, playerId) => message match {
      case _ =>
      //case sb.Animation(hand) if hand == Hand.MainHand => sendToPlayers(cb.Animation(playerId, AnimationType.SwingMainArm))
      //case sb.Animation(hand) if hand == Hand.OffHand => sendToPlayers(cb.Animation(playerId, AnimationType.SwingOffHand))
    }
    case msg:PlayerDiggingWithItem => diggingManager forward msg
    case msg => sendToPlayers(msg)
  }

  private def sendToPlayers(obj: Any) = {
    context.actorSelection("/user/world/Player*") ! ForwardToClient(obj)
  }

  private def sentToPlayer(username: String, obj: Any): Unit = {
    context.actorSelection("/user/world/Player-" + username) ! ForwardToClient(obj)
  }

}

object World {

  def props: Props = Props(new World)
  def name: String = "world"

}
