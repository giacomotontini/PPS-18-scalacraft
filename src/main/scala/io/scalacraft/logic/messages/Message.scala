package io.scalacraft.logic.messages

import java.util.UUID

import akka.actor.ActorRef
import io.scalacraft.packets.DataTypes.Position
import io.scalacraft.packets.clientbound.PlayPackets.CollectItem
import io.scalacraft.packets.serverbound.PlayPackets.{PlayerBlockPlacement, PlayerDigging}

sealed trait Message

object Message {

  case class BlockPlacedByUser(playerBlockPlacement: PlayerBlockPlacement, itemId: Int, username: String) extends Message

  /* --------------------------------------------- Region and chunks --------------------------------------------- */
  sealed trait RegionMessage extends Message
  case class  RequestChunkData(chunkX: Int, chunkZ: Int, fullChunk: Boolean = true) extends RegionMessage
  case object ChunkNotPresent extends RegionMessage
  case class  RequestBlockState(position: Position) extends Message
  case class  FindFirstSolidBlockPositionUnder(position: Position) extends RegionMessage

  case class  RegisterUser(username: String) extends Message
  case class  UserRegistered(entityId: Int, uuid: UUID, player: ActorRef) extends Message
  case class  RequestJoinGame(entityId: Int, userContext: ActorRef) extends Message

  // Sent by Player to inform world that player started playing
  case class JoiningGame(playerId: Int) extends Message

  // Sent by Player to inform world that player stopped playing
  case class LeavingGame(playerId: Int) extends Message
  case object UserDisconnected extends Message
  case object RemovePlayer extends Message

  // Sent by UserContext to World to ask the number of players online
  case object RequestOnlinePlayers extends Message

  case class  OnlinePlayers(number: Int) extends Message
  
  case object RequestEntityId extends Message

  /* --------------------------------------------- User --------------------------------------------- */
  case class PlayerMoved(playerId: Int, position: Position) extends Message

  /* --------------------------------------------- Digging manager --------------------------------------------- */
  case class PlayerDiggingHoldingItem(playerId: Int, playerPosition: Position, playerDiggingPacket: PlayerDigging,
                                      holdingItemId: Option[Int]) extends Message
  case class BreakBlockAtPosition(position: Position) extends Message
  case class BlockBrokenAtPosition(position: Position) extends Message

  /* --------------------------------------------- Drop manager --------------------------------------------- */
  case class DropItems(itemId: Int, quantity: Int, blockPosition: Position, playerId: Int, playerPosition: Position)
    extends Message
  case class CollectItemWithType(collectItem: CollectItem, itemId: Int) extends Message

  /* --------------------------------------------- World --------------------------------------------- */
  case class SendToAllExclude(playerId: Int, obj: Any) extends Message
  case class SendToPlayer(playerId: Int, obj: Any) extends Message
  case class SendToAll(obj: Any) extends Message
  case class ForwardToClient(obj: Any) extends Message

}
