package io.scalacraft.logic.messages

import java.util.UUID

import akka.actor.ActorRef
import io.scalacraft.logic.InventoryItem
import io.scalacraft.packets.DataTypes.Position
import io.scalacraft.packets.serverbound.PlayPackets.{Animation, PlayerBlockPlacement}

trait Message

object Message {

  case class ForwardToClient(obj:Any) extends Message
  case class BlockPlacedByUser(playerBlockPlacement: PlayerBlockPlacement, itemId: Int, username: String) extends Message
  case class BlockBreakAtPosition(position: Position, playerId: Int) extends Message
  case class PlayerAnimation(username:String, entityId: Int, animation: Animation)

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

  case class  OnlinePlayers(number: Int) extends Message
  
  case object RequestEntityId extends Message


  /* ------------------------------------------------ Inventories -------------------------------------------------- */

  case class AddItem(inventoryItem: InventoryItem) extends Message

  case class RemoveItem(slotIndex: Int, inventoryItem: InventoryItem) extends Message

  case class RetrieveAllItems() extends Message

  case class RetrieveHeldItemId() extends Message

  case class UseHeldItem() extends Message


}
