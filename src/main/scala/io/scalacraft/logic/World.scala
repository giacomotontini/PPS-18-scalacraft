package io.scalacraft.logic

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.typesafe.scalalogging.LazyLogging
import io.scalacraft.core.fsm.ConnectionState.PlayState
import io.scalacraft.logic.messages.Message.{ChunkNotPresent, PlayerLogged, RequestChunkData}
import io.scalacraft.loaders.Regions
import net.querz.nbt.mca.MCAUtil

class World extends Actor with LazyLogging with ActorLogging {

  private var regions: Map[(Int, Int), ActorRef] = _

  override def preStart(): Unit = {
    try {
      log.debug("Loading regions..")
      regions = Regions.loadRegions() map {
        case ((x, y), file) => (x, y) -> context.actorOf(Region.props(file), Region.name(x, y))
      }
    } catch {
      case e: Exception => logger.error("Error loading the world", e)
    }
  }

  override def receive: Receive = {
    case PlayerLogged(playState: PlayState) => context.actorOf(Player.props(playState))
    case request @ RequestChunkData(chunkX, chunkZ, _) =>
      val (relativeX, relativeZ) = (MCAUtil.chunkToRegion(chunkX), MCAUtil.chunkToRegion(chunkZ))
      if (regions.contains((relativeX, relativeZ))) {
        regions((relativeX, relativeZ)) forward request
      } else {
        log.warning(s"Region ($relativeX,$relativeZ) not loaded")
        sender ! ChunkNotPresent
      }
  }

}

object World {

  def props: Props = Props(new World)
  def name: String = "world"

}
