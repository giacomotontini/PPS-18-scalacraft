package io.scalacraft.logic.commons

import java.util.UUID

import akka.actor.ActorRef
import io.scalacraft.logic.Region.Light
import io.scalacraft.logic.commons.Message.SkyUpdateState.SkyUpdateState
import io.scalacraft.logic.inventories.InventoryItem
import io.scalacraft.packets.DataTypes.{EntityId, Position, Slot}
import io.scalacraft.packets.clientbound.PlayPackets.{CollectItem, EntityProperties, SoundEffect, SpawnPlayer}
import io.scalacraft.packets.serverbound.PlayPackets.{PlayerBlockPlacement, PlayerDigging, UseEntity}
import net.querz.nbt.CompoundTag

sealed trait Message

object Message {

  /* --------------------------------------------- Region and chunks --------------------------------------------- */

  case class RequestChunkData(chunkX: Int, chunkZ: Int, fullChunk: Boolean = true) extends Message

  case object ChunkNotPresent extends Message

  case class RequestBlockState(position: Position) extends Message

  case class RequestLight(position: Position) extends Message

  case class FindFirstSolidBlockPositionUnder(position: Position) extends Message

  case class ChangeBlockState(position: Position, tag: CompoundTag)

  case class ChangeLight(position: Position, light: Light)

  /* --------------------------------------------- Player  --------------------------------------------- */

  case class RegisterUser(username: String) extends Message

  case class UserRegistered(entityId: EntityId, uuid: UUID, player: ActorRef) extends Message

  case class RequestJoinGame(entityId: EntityId, userContext: ActorRef) extends Message

  /**
   * Sent by Player to inform world that player started playing
   */
  case class PlayerJoiningGame(playerId: EntityId, username: String) extends Message

  /**
   * Sent by Player to inform world that player stopped playing
   */
  case class PlayerLeavingGame(playerId: EntityId, username: String) extends Message

  case class PlayerSpawning(playerId: EntityId, spawnPacket: SpawnPlayer, properties: EntityProperties) extends Message

  case object UserDisconnected extends Message

  case object RemovePlayer extends Message

  /**
   * Sent by UserContext to World to ask the number of players online
   */
  case object RequestOnlinePlayers extends Message

  case object RequestEntityId extends Message

  case class PlayerMoved(playerId: EntityId, position: Position) extends Message

  /**
   * Sent by PlayerInventory to inform player that the held item is changed and must be propagated to other clients (via world
   */
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

  case class PlayerDiggingHoldingItem(playerId: EntityId, playerPosition: Position, playerDiggingPacket: PlayerDigging,
                                      holdingItemId: Option[Int]) extends Message

  case class BreakBlockAtPosition(position: Position) extends Message

  case class BlockBrokenAtPosition(position: Position) extends Message

  /* --------------------------------------------- Drop manager -------------------------------------------------- */

  case class DropItems(itemId: Int, quantity: Int, blockPosition: Position, playerId: EntityId,
                       playerPosition: Position) extends Message

  case class CollectItemWithType(collectItem: CollectItem, itemId: Int) extends Message

  /* --------------------------------------------- World --------------------------------------------------------- */

  case class PlayerPlaceBlockWithItemId(playerId: EntityId, packet: PlayerBlockPlacement, itemId: Int) extends Message

  case object RequestSpawnPacket extends Message

  /* --------------------------------------------- General --------------------------------------------------------- */

  case class SendToAllExclude(playerId: EntityId, obj: Any) extends Message

  case class SendToPlayer(playerId: EntityId, obj: Any) extends Message

  case class SendToAll(obj: Any) extends Message

  case class ForwardToClient(obj: Any) extends Message

  case class AskResponse(sender: ActorRef, responseObject: Any) extends Message

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
