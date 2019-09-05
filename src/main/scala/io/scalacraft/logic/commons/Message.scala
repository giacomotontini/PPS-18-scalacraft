package io.scalacraft.logic.commons

import java.util.UUID

import akka.actor.ActorRef
import io.scalacraft.logic.Region.Light
import io.scalacraft.logic.commons.Message.SkyUpdateState.SkyUpdateState
import io.scalacraft.logic.inventories.InventoryItem
import io.scalacraft.core.packets.DataTypes.{EntityId, Position, Slot}
import io.scalacraft.core.packets.clientbound.PlayPackets.{CollectItem, EntityProperties, SpawnPlayer}
import io.scalacraft.core.packets.serverbound.PlayPackets.{PlayerBlockPlacement, PlayerDigging, UseEntity}
import net.querz.nbt.CompoundTag

sealed trait Message

object Message {

  /* --------------------------------------------- Region and chunks --------------------------------------------- */

  /**
   * To be used to request data of a chunk to a region. Responses can be:<br>
   * - [[io.scalacraft.core.packets.clientbound.PlayPackets.ChunkData ChunkData]] if the request is successful<br>
   * - [[io.scalacraft.logic.commons.Message.ChunkNotPresent ChunkNotPresent]] if the chunk in not loaded
   *
   * @param chunkX the X coordinate of the chunk in the region
   * @param chunkZ the Z coordinate of the chunk in the region
   * @param fullChunk true if all data need to be loaded
   */
  case class RequestChunkData(chunkX: Int, chunkZ: Int, fullChunk: Boolean = true) extends Message

  /**
   * A reply to [[io.scalacraft.logic.commons.Message.RequestChunkData RequestChunkData]] that means that a chunk is
   * not loaded.
   */
  case object ChunkNotPresent extends Message

  /**
   * To be used to request the block state of a certain block. The response is [[net.querz.nbt.CompoundTag CompoundTag]].
   *
   * @param position the block position
   */
  case class RequestBlockState(position: Position) extends Message

  /**
   * To be used to request the light of a certain block. The response is [[io.scalacraft.logic.Region.Light Light]].
   *
   * @param position the block position
   */
  case class RequestLight(position: Position) extends Message

  /**
   * To be used to request the first solid block position (e.g. not air, water, etc..) under a certain block. The
   * response is [[io.scalacraft.core.packets.DataTypes.Position Position]].
   *
   * @param position the initial block position
   */
  case class FindFirstSolidBlockPositionUnder(position: Position) extends Message

  /**
   * To be used to change the block state of a certain block. There are no responses.
   *
   * @param position the position of the block to change
   * @param tag the new [[net.querz.nbt.CompoundTag CompoundTag]] to set
   */
  case class ChangeBlockState(position: Position, tag: CompoundTag)

  /**
   * To be used to change the light of a certain block. There are no responses.
   *
   * @param position the position of the block to change
   * @param light the new [[io.scalacraft.logic.Region.Light Light]] to set
   */
  case class ChangeLight(position: Position, light: Light)

  /* --------------------------------------------- Player  --------------------------------------------- */

  /**
   * To be used to register a new player in the world. The response is a
   * [[io.scalacraft.logic.commons.Message.UserRegistered UserRegistered]].
   *
   * @param username the username of the player
   */
  case class RegisterUser(username: String) extends Message

  /**
   * Used as response of a [[io.scalacraft.logic.commons.Message.RegisterUser RegisterUser]] request.
   *
   * @param entityId the entity id of the newly registered user
   * @param uuid the uuid of the newly registered user
   * @param player the [[akka.actor.ActorRef ActorRef]] of the newly registered user
   */
  case class UserRegistered(entityId: EntityId, uuid: UUID, player: ActorRef) extends Message

  /**
   * Used to tell a player that can join the game.
   *
   * @param entityId the player entity id
   * @param userContext the context of the player
   */
  case class RequestJoinGame(entityId: EntityId, userContext: ActorRef) extends Message

  /**
   * Used to inform world that a player started playing.
   *
   * @param playerId the entity id of the player
   * @param username the username of the player
   */
  case class PlayerJoiningGame(playerId: EntityId, username: String) extends Message

  /**
   * Used to inform world that a player stopped playing.
   *
   * @param playerId the entity id of the player
   * @param username the username of the player
   */
  case class PlayerLeavingGame(playerId: EntityId, username: String) extends Message

  /**
   * Used to inform world that a player has just spawned.
   *
   * @param playerId    the entity id of the player
   * @param spawnPacket the [[io.scalacraft.core.packets.clientbound.PlayPackets.SpawnPlayer SpawnPlayer]] packet
   * @param properties  the player properties
   */
  case class PlayerSpawning(playerId: EntityId, spawnPacket: SpawnPlayer, properties: EntityProperties) extends Message

  /**
   * Used to inform the user context that the user disconnected and his connection is closed.
   */
  case object UserDisconnected extends Message

  /**
   * Used to inform the player that the player disconnected and must be removed from world.
   */
  case object RemovePlayer extends Message

  /**
   * Used to request the number of online players. The response is the number of online players.
   */
  case object RequestOnlinePlayers extends Message

  /**
   * Used to request a new id for an entity. The response is the newly generated entity id.
   */
  case object RequestEntityId extends Message

  /**
   * Used to inform world that a player is moved.
   *
   * @param playerId the entity id of the player that moves
   * @param position the new position of the player
   */
  case class PlayerMoved(playerId: EntityId, position: Position) extends Message

  case class EquipmentChanged(equipment: Slot) extends Message

  /* --------------------------------------------- Inventories --------------------------------------------------- */
  case object LoadInventory extends Message

  case class AddItem(inventoryItem: InventoryItem) extends Message

  case class RemoveItem(slotIndex: Int, inventoryItem: InventoryItem) extends Message

  case object RetrieveAllItems extends Message

  case object RetrieveInventoryItems extends Message

  case object RetrieveHeldItemId extends Message

  case object UseHeldItem extends Message

  case class PopulatePlayerInventory(inventory: List[Option[InventoryItem]])

  case class InventoryDropItems(itemId: Int, quantity: Int) extends Message

  /* --------------------------------------------- Digging manager ----------------------------------------------- */

  /**
   * Used to inform [[io.scalacraft.logic.DiggingManager DiggingManager]] that a player does a digging action on a block.
   * In this message is specified also the item that the player used to dig.
   *
   * @param playerId the entity id of the player
   * @param playerPosition the position of the player
   * @param playerDiggingPacket the [[io.scalacraft.core.packets.serverbound.PlayPackets.PlayerDigging PlayerDigging]]
   *                            packet received by client
   * @param holdingItemId the optional item id hold by player
   */
  case class PlayerDiggingHoldingItem(playerId: EntityId, playerPosition: Position, playerDiggingPacket: PlayerDigging,
                                      holdingItemId: Option[Int]) extends Message

  /**
   * Used to inform [[io.scalacraft.logic.DropManager DropManager]] that a block at a certain position is broken.
   *
   * @param position the position of the broken block
   */
  case class BlockBrokenAtPosition(position: Position) extends Message

  /* --------------------------------------------- Drop manager -------------------------------------------------- */

  /**
   * Used to inform [[io.scalacraft.logic.DropManager DropManager]] that a new item must be dropped.
   *
   * @param itemId the id of the item
   * @param quantity the quantity
   * @param blockPosition the position where the drop must fall
   * @param playerId the player id of the player which has accomplished the drop operation
   * @param playerPosition the position of the player which has accomplished the drop operation
   */
  case class DropItems(itemId: Int, quantity: Int, blockPosition: Position, playerId: EntityId,
                       playerPosition: Position) extends Message

  /**
   * Used to inform the player that a drop can be collected.
   *
   * @param collectItem the [[io.scalacraft.core.packets.clientbound.PlayPackets.CollectItem CollectItem]] packet relative
   *                    to the item to collect
   * @param itemId      the id of the item to collect
   */
  case class CollectItemWithType(collectItem: CollectItem, itemId: Int) extends Message

  /* --------------------------------------------- World --------------------------------------------------------- */

  /**
   * Used by player to inform that a new block is placed in the world.
   *
   * @param playerId the id of the player who place the new block
   * @param packet   the [[io.scalacraft.core.packets.serverbound.PlayPackets.PlayerBlockPlacement PlayerBlockPlacement]] packet
   * @param itemId   the id of the item to place
   */
  case class PlayerPlaceBlockWithItemId(playerId: EntityId, packet: PlayerBlockPlacement, itemId: Int) extends Message

  /**
   * Used to request to a player his [[io.scalacraft.core.packets.clientbound.PlayPackets.SpawnPlayer SpawnPlayer]] packet.
   */
  case object RequestSpawnPacket extends Message

  /* --------------------------------------------- General --------------------------------------------------------- */

  /**
   * Used to send a packet to all player excluding one.
   *
   * @param playerId the id of the player to exclude
   * @param obj the packet to send
   */
  case class SendToAllExclude(playerId: EntityId, obj: Any) extends Message

  /**
   * Used to send a packet to a specific player.
   *
   * @param playerId the id of the player
   * @param obj the packet to send
   */
  case class SendToPlayer(playerId: EntityId, obj: Any) extends Message

  /**
   * Used to send a packet to all players.
   *
   * @param obj the packet to send
   */
  case class SendToAll(obj: Any) extends Message

  /**
   * Used to forward a packet to the player's [[io.scalacraft.logic.UserContext UserContext]]
   *
   * @param obj the packet to forward
   */
  case class ForwardToClient(obj: Any) extends Message

  /**
   * A generic ask response that carries the reference of the actor that reply.
   *
   * @param sender the [[akka.actor.ActorRef ActorRef]] of the actor that reply
   * @param responseObject the response message
   */
  case class AskResponse(sender: ActorRef, responseObject: Any) extends Message

  /**
   * A generic ask reply that carries the reference of the actor that make the request.
   *
   * @param sender the [[akka.actor.ActorRef ActorRef]] of the actor that make the request
   * @param requestObject the request message
   */
  case class AskRequest(sender: ActorRef, requestObject: Any) extends Message

  /* ------------------------------------------- Creatures Spawn/Despawn ------------------------------------------- */

  case class SpawnCreaturesInChunk(chunkX: Int, chunkZ: Int) extends Message

  case class PlayerUnloadedChunk(chunkX: Int, chunkZ: Int) extends Message

  case class RequestCreatureInChunk(chunkX: Int, chunkZ: Int) extends Message

  case class DespawnCreature(chunkX: Int, chunkZ: Int) extends Message

  case class RequestSpawnPoints(chunkX: Int, chunkZ: Int) extends Message

  object SkyUpdateState extends Enumeration {
    type SkyUpdateState = Value
    val Sunrise, Noon, Sunset, MidNight = Value

    def timeUpdateStateFromTime(timeOfDay: Long): SkyUpdateState = timeOfDay match {
      case _: Long if timeOfDay >= 0 && timeOfDay < 6000 => Sunrise
      case _: Long if timeOfDay >= 6000 && timeOfDay < 12000 => Noon
      case _: Long if timeOfDay >= 12000 && timeOfDay < 18000 => Sunset
      case _: Long if timeOfDay >= 18000 && timeOfDay < 24000 => MidNight
    }
  }

  case class SkyStateUpdate(state: SkyUpdateState) extends Message

  case class UseEntityWithItem(useEntity: UseEntity, itemId: Int)

  case class EntityDead(entityId: Int)

  /* --------------------------------------------- Creatures AI --------------------------------------------- */

  case class RequestNearbyPoints(x: Int, y: Int, z: Int, oldX: Int, oldZ: Int) extends Message

}
