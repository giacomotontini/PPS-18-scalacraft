package io.scalacraft.logic.messages

import java.util.UUID

import akka.actor.ActorRef

trait Message

object Message {

  /* --------------------------------------------- Region and chunks --------------------------------------------- */
  case class  RequestChunkData(chunkX: Int, chunkZ: Int, fullChunk: Boolean = true) extends Message
  case object ChunkNotPresent extends Message

  case class  RegisterUser(username: String, userContext: ActorRef) extends Message
  case class  UserRegistered(uuid: UUID, player: ActorRef) extends Message
  case object CanJoinGame


}
