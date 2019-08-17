package io.scalacraft.logic.messages

import java.util.UUID

import akka.actor.ActorRef
import io.scalacraft.core.marshalling.annotations.PacketAnnotations.{boxed, fromContext, short}
import io.scalacraft.logic.messages.Message.SkyUpdateState.SkyUpdateState
import io.scalacraft.packets.DataTypes.{Angle, Position}
import io.scalacraft.packets.Entities.MobEntity

trait Message

object Message {

  case class AskResponse(sender: ActorRef, responseObject: Any) extends Message
  case class AskRequest(sender: ActorRef, requestObject: Any) extends Message
  /* --------------------------------------------- Region and chunks --------------------------------------------- */
  case class RequestChunkData(chunkX: Int, chunkZ: Int, fullChunk: Boolean = true) extends Message

  case object ChunkNotPresent extends Message

  case class  RegisterUser(username: String) extends Message
  case class  UserRegistered(entityId: Int, uuid: UUID, player: ActorRef) extends Message
  case class  RequestJoinGame(entityId: Int, userContext: ActorRef) extends Message

  // Sent by Player to inform world that player started playing
  case object JoiningGame extends Message

  // Sent by Player to inform world that player stopped playing
  case object LeavingGame extends Message
  case object UserDisconnected extends Message
  case object RemovePlayer extends Message

  // Sent by UserContext to World to ask the number of players online
  case object RequestOnlinePlayers extends Message

  case object RequestEntityId extends Message

  case class RequestMobsInChunk(chunkX: Int, chunkZ: Int) extends Message

  case class PlayerUnloadedChunk(chunkX: Int, chunkZ: Int) extends Message

  case class GetCreatureInChunk(chunkX: Int, chunkZ: Int) extends Message

  case class RequestSpawnPoints(chunkX: Int, chunkZ: Int) extends Message

  case class DespawnCreature(chunkX: Int, chunkZ: Int) extends Message

  object SkyUpdateState extends Enumeration {
    type SkyUpdateState = Value
    val Sunrise, Noon, Sunset, MidNight = Value

    def timeUpdateStateFromTime(timeOfDay: Long): SkyUpdateState = {
      timeOfDay match {
        case _: Long if timeOfDay>=0 && timeOfDay < 6000  => Sunrise
        case _: Long if timeOfDay>=6000 && timeOfDay < 12000  => Noon
        case _: Long if timeOfDay>=12000 && timeOfDay < 18000  => Sunset
        case _: Long if timeOfDay>=18000 && timeOfDay < 24000  => MidNight
      }
    }
  }
  case class SkyStateUpdate(state: SkyUpdateState) extends Message

}
