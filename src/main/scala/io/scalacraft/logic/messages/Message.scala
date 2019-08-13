package io.scalacraft.logic.messages

import java.util.UUID

import akka.actor.ActorRef
import io.scalacraft.core.marshalling.annotations.PacketAnnotations.{boxed, fromContext, short}
import io.scalacraft.packets.DataTypes.{Angle, Position}
import io.scalacraft.packets.Entities.MobEntity

trait Message

object Message {

  /* --------------------------------------------- Region and chunks --------------------------------------------- */
  case class RequestChunkData(chunkX: Int, chunkZ: Int, fullChunk: Boolean = true) extends Message

  case object ChunkNotPresent extends Message

  case class RegisterUser(username: String, userContext: ActorRef) extends Message

  case class UserRegistered(uuid: UUID, player: ActorRef) extends Message

  case object CanJoinGame

  // Sent by Player to inform world that player started playing
  case object JoiningGame extends Message

  // Sent by Player to inform world that player stopped playing
  case object LeavingGame extends Message
  case object UserDisconnected extends Message
  case object RemovePlayer extends Message

  // Sent by UserContext to World to ask the number of players online
  case object RequestOnlinePlayers extends Message

  case class  OnlinePlayers(number: Int) extends Message
  
  /* --------------------------------------------- Mobs Spawning --------------------------------------------- */

  case class RequestMobsInChunk(chunkX: Int, chunkZ: Int) extends Message

  case class PlayerExitedFromChunk(chunkX: Int, chunkZ: Int) extends Message

  case class GetCreatureInChunk(chunkX: Int, chunkZ: Int)

  case class RequestSpawnPoints(chunkX: Int, chunkZ: Int) extends Message

  case class DespawnCreature(chunkX: Int, chunkZ: Int) extends Message


}
