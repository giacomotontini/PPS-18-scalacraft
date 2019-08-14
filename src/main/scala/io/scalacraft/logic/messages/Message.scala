package io.scalacraft.logic.messages

import java.util.UUID

import akka.actor.ActorRef

trait Message

object Message {

  /* --------------------------------------------- Region and chunks --------------------------------------------- */
  case class  RequestChunkData(chunkX: Int, chunkZ: Int, fullChunk: Boolean = true) extends Message
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

}
