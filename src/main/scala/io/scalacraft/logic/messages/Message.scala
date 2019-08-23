package io.scalacraft.logic.messages

import java.util.UUID

import akka.actor.ActorRef
import io.scalacraft.packets.DataTypes.{EntityId, ItemId, Position}
import io.scalacraft.packets.clientbound.PlayPackets.{CollectItem, EntityProperties, SpawnPlayer}
import io.scalacraft.packets.serverbound.PlayPackets.{PlayerBlockPlacement, PlayerDigging}
import net.querz.nbt.CompoundTag

sealed trait Message

object Message {

  /* --------------------------------------------- Region and chunks --------------------------------------------- */
  sealed trait RegionMessage extends Message
  case class  RequestChunkData(chunkX: Int, chunkZ: Int, fullChunk: Boolean = true) extends RegionMessage
  case object ChunkNotPresent extends RegionMessage
  case class  RequestBlockState(position: Position) extends Message
  case class  FindFirstSolidBlockPositionUnder(position: Position) extends RegionMessage
  case class  ChangeBlockState(position: Position, tag: CompoundTag)

  case class  RegisterUser(username: String) extends Message
  case class  UserRegistered(entityId: EntityId, uuid: UUID, player: ActorRef) extends Message
  case class  RequestJoinGame(entityId: EntityId, userContext: ActorRef) extends Message

  // Sent by Player to inform world that player started playing
  case class PlayerJoiningGame(playerId: EntityId, username: String) extends Message

  // Sent by Player to inform world that player stopped playing
  case class  PlayerLeavingGame(playerId: EntityId, username: String) extends Message
  case class  PlayerSpawning(playerId: EntityId, spawnPacket: SpawnPlayer, properties: EntityProperties) extends Message
  case object UserDisconnected extends Message
  case object RemovePlayer extends Message

  // Sent by UserContext to World to ask the number of players online
  case object RequestOnlinePlayers extends Message

  case class  OnlinePlayers(number: Int) extends Message
  
  case object RequestEntityId extends Message

  /* --------------------------------------------- User --------------------------------------------- */
  case class PlayerMoved(playerId: EntityId, position: Position) extends Message

  /* --------------------------------------------- Digging manager --------------------------------------------- */
  case class PlayerDiggingHoldingItem(playerId: EntityId, playerPosition: Position, playerDiggingPacket: PlayerDigging,
                                      holdingItemId: Option[ItemId]) extends Message
  case class BreakBlockAtPosition(position: Position) extends Message
  case class BlockBrokenAtPosition(position: Position) extends Message

  /* --------------------------------------------- Drop manager --------------------------------------------- */
  case class DropItems(itemId: ItemId, quantity: Int, blockPosition: Position, playerId: EntityId,
                       playerPosition: Position) extends Message
  case class CollectItemWithType(collectItem: CollectItem, itemId: ItemId) extends Message

  /* --------------------------------------------- World --------------------------------------------- */
  case class PlayerPlaceBlockWithItemId(playerId: EntityId, packet: PlayerBlockPlacement, itemId: Int) extends Message
  case object RequestSpawnPacket extends Message
  case class SendToAllExclude(playerId: EntityId, obj: Any) extends Message
  case class SendToPlayer(playerId: EntityId, obj: Any) extends Message
  case class SendToAll(obj: Any) extends Message
  case class ForwardToClient(obj: Any) extends Message

}
