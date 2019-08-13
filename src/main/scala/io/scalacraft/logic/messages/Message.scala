package io.scalacraft.logic.messages

import java.util.UUID

import akka.actor.ActorRef
import io.scalacraft.packets.DataTypes.Position

trait Message

object Message {

  case class ForwardToClient(obj:Any) extends Message
  case class ForwardToWorld(obj: Any, entityId: Int) extends Message
  /* --------------------------------------------- Region and chunks --------------------------------------------- */
  case class  RequestChunkData(chunkX: Int, chunkZ: Int, fullChunk: Boolean = true) extends Message
  case object ChunkNotPresent extends Message

  case class  RegisterUser(username: String, userContext: ActorRef) extends Message
  case class  UserRegistered(uuid: UUID, player: ActorRef) extends Message
  case object CanJoinGame extends Message

  case class BlockBreakAtPosition(position: Position, playerId: Int) extends Message

}
