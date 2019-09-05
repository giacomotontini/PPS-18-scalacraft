package io.scalacraft.logic

import java.util.UUID

import akka.actor.{ActorRef, Cancellable, Props, Stash}
import akka.pattern._
import io.scalacraft.loaders.Blocks
import io.scalacraft.logic.commons.Message._
import io.scalacraft.logic.commons.Traits.EnrichedActor
import io.scalacraft.logic.inventories.actors.{CraftingTableActor, PlayerInventoryActor}
import io.scalacraft.logic.inventories.{InventoryItem, PlayerInventory}
import io.scalacraft.misc.ServerConfiguration
import io.scalacraft.core.packets.DataTypes.{Angle, Position}
import io.scalacraft.core.packets.Entities
import io.scalacraft.core.packets.clientbound.PlayPackets._
import io.scalacraft.core.packets.clientbound.{PlayPackets => cb}
import io.scalacraft.core.packets.serverbound.PlayPackets._
import io.scalacraft.core.packets.serverbound.{PlayPackets => sb}
import net.querz.nbt.CompoundTag

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Random, Success}


/**
 * The actor that represent a player in the game. It handle the movements in the world, the dynamic loading of chunks,
 * the status and the settings of the player, the animations and the sounds that the player produce.
 *
 * @param username the player username
 * @param playerUUID the player uuid
 * @param serverConfiguration the global server configuration
 */
class Player(username: String, playerUUID: UUID, serverConfiguration: ServerConfiguration) extends EnrichedActor
  with Stash {

  import Player._

  private val world = context.parent
  private var userContext: ActorRef = _
  private var activeInventories: Map[Int, ActorRef] = Map(PlayerInventory.Id -> context.actorOf(PlayerInventoryActor.props(self)))
  private val randomGenerator = Random
  private var loadedChunks: Set[(Int, Int)] = Set()
  private var lastKeepAliveId: Long = _
  private var keepAliveCancellable: Cancellable = _
  private var lastWindowId = 1

  private var playerEntityId = 0
  private val worldDimension = WorldDimension.Overworld
  private var posX: Double = -10
  private var posY: Double = 70
  private var posZ: Double = 20
  private var yaw: Float = 0.0f
  private var pitch: Float = 0.0f
  private var onGround = true
  private val health = 20
  // private var foodHealth = 20
  private var mainHand: MainHand = _
  private var locale: String = _
  private var viewDistance: Int = _ //render distance in chunks
  private val entityMetadata: Entities.Player = new Entities.Player

  override def receive: Receive = preStartBehaviour

  private def preStartBehaviour: Receive = {
    case RequestJoinGame(playerEntityId, playerUserContext) =>
      this.playerEntityId = playerEntityId
      userContext = playerUserContext

      userContext ! JoinGame(playerEntityId, serverConfiguration.gameMode, worldDimension,
        serverConfiguration.serverDifficulty, serverConfiguration.maxPlayers, serverConfiguration.levelTypeBiome,
        serverConfiguration.reducedDebugInfo)
      world ! PlayerJoiningGame(playerEntityId, username)

    case clientSettings: sb.ClientSettings =>
      locale = clientSettings.locale
      mainHand = clientSettings.mainHand
      viewDistance = if (clientSettings.viewDistance > ServerConfiguration.MaxViewDistance)
        ServerConfiguration.MaxViewDistance else clientSettings.viewDistance

      loadChunksAndMobs() onComplete {
        case Success(_) =>
          val teleportId = randomGenerator.nextInt
          val flags = playerPositionAndLookFlags(xRelative = false, yRelative = false, zRelative = false, Y_ROT = false,
            X_ROT = false)
          val positionAndLook = cb.PlayerPositionAndLook(posX, posY, posZ, yaw, pitch, flags, teleportId)
          entityMetadata.health = health
          userContext ! cb.SpawnPosition(Position(posX, posY, posZ))
          userContext ! positionAndLook

          context.become(confirmTeleport(positionAndLook))
        case Failure(e) => log.error(e, "Cannot load chunks.")
      }

    case ClientStatus(ClientStatusAction.PerformRespawn) => log.warning("Respawn not to be handled on login.")
    case ClientStatus(ClientStatusAction.RequestStats) => log.warning("User request statistics, not handled.")
    case sb.PluginMessage(brand, data) => userContext ! cb.PluginMessage(brand, data)
    case _ => stash()
  }

  private def confirmTeleport(positionAndLook: cb.PlayerPositionAndLook): Receive = {
    case sb.TeleportConfirm(teleportId) if positionAndLook.teleportId == teleportId =>
      val entityProperties = EntityProperties(playerEntityId, PlayerEntityProperties)
      world ! PlayerSpawning(playerEntityId, spawnPlayerPacket, entityProperties)
      activeInventories(PlayerInventory.Id) ! LoadInventory
      keepAliveCancellable = context.system.scheduler.schedule(0 millis, 10 seconds) {
        lastKeepAliveId = System.currentTimeMillis
        userContext ! cb.KeepAlive(lastKeepAliveId)
      }
      context.become(playingBehaviour)
      unstashAll()
    case _ => stash()
  }

  private def playingBehaviour: Receive = {

    case sb.KeepAlive(keepAliveId) if keepAliveId == lastKeepAliveId => // ok
    case sb.KeepAlive(keepAliveId) if keepAliveId != lastKeepAliveId => log.warning("Client keep alive id invalid.")

    case RequestSpawnPacket => sender ! spawnPlayerPacket

    case sb.Player(_onGround) => updatePositionAndLook(onGround = _onGround)
    case sb.PlayerPosition(x, feetY, z, _onGround) => updatePositionAndLook(Some(x, feetY, z), onGround = _onGround)
    case sb.PlayerPositionAndLook(x, feetY, z, yaw, pitch, onGround) =>
      updatePositionAndLook(Some(x, feetY, z), Some(yaw, pitch), onGround)
    case sb.PlayerLook(yaw, pitch, _onGround) =>
      updatePositionAndLook(direction = Some(yaw, pitch), onGround = _onGround)

    case packet: sb.HeldItemChange =>
      activeInventories(PlayerInventory.Id) forward packet
    case playerDigging: sb.PlayerDigging =>
      (activeInventories(PlayerInventory.Id) ? RetrieveHeldItemId).mapTo[Option[Int]] onComplete {
        case Success(heldItemId) =>
          world ! PlayerDiggingHoldingItem(playerEntityId, Position(posX, posY, posZ), playerDigging, heldItemId)
        case Failure(exception) => log.error(exception, "Failed to retrieve held item.")
      }
    case packet: sb.PlayerBlockPlacement =>
      (activeInventories(PlayerInventory.Id) ? UseHeldItem).mapTo[Option[Int]] onComplete {
        case Success(Some(itemId)) => world ! PlayerPlaceBlockWithItemId(playerEntityId, packet, itemId)
        case Success(None) =>
          (world ? RequestBlockState(packet.position)).mapTo[CompoundTag] onComplete {
            case Success(blockStateCompound) =>
              val block = Blocks.blockFromCompoundTag(blockStateCompound)
              handleInteractionOn(block)
            case Failure(exception) => log.error(exception, "Failed to retrieve block's state on interaction")
          }
        case Failure(ex) => log.error(ex, "Failed to retrieve placed block type.")
      }
    case sb.Animation(Hand.MainHand) =>
      world ! SendToAllExclude(playerEntityId, cb.Animation(playerEntityId, AnimationType.SwingMainArm))
    case sb.Animation(Hand.OffHand) =>
      world ! SendToAllExclude(playerEntityId, cb.Animation(playerEntityId, AnimationType.SwingOffHand))

    case useEntity: UseEntity =>
      (activeInventories(PlayerInventory.Id) ? RetrieveHeldItemId).mapTo[Option[Int]] onComplete {
        case Success(heldItemId) =>
          world ! UseEntityWithItem(useEntity, heldItemId.getOrElse(0))
        case Failure(exception) => log.error(exception, "Failed to retrieve held item.")
      }

    case ForwardToClient(packet) => packet match {
      case CollectItemWithType(collectItem, itemId) =>
        userContext forward collectItem
        if (collectItem.collectorEntityId == playerEntityId) {
          activeInventories(PlayerInventory.Id) ! AddItem(InventoryItem(itemId, collectItem.pickUpItemCount))
        }
      case EquipmentChanged(equipment) =>
        world ! SendToAllExclude(playerEntityId, EntityEquipment(playerEntityId, EquipmentSlot.MainHand, equipment))
      case _ => userContext forward packet
    }

    case packet: ClickWindow if activeInventories.contains(packet.windowId) =>
      activeInventories(packet.windowId) forward packet
    case packet: CloseWindow if activeInventories.contains(packet.windowId) =>
      activeInventories(packet.windowId) forward packet
    case InventoryDropItems(itemId, quantity) =>
      val x = -math.sin(math.toRadians(yaw)) * 3
      val z = math.cos(math.toRadians(yaw)) * 3
      val itemDropPosition = Position(posX + x.toInt, posY, posZ + z.toInt)
      world ! DropItems(itemId, quantity, itemDropPosition, playerEntityId, Position(posX, posY, posZ))
    case RemovePlayer =>
      world ! PlayerLeavingGame(playerEntityId, username)
      reset()

    case entityRelativeMove: EntityRelativeMove => userContext ! entityRelativeMove
    case entityLookAndRelativeMove: EntityLookAndRelativeMove => userContext ! entityLookAndRelativeMove
    case entityVelocity: EntityVelocity => userContext ! entityVelocity
    case entityLook: EntityLook => userContext ! entityLook
    case entityHeadLook: EntityHeadLook => userContext ! entityHeadLook
    case unhandled => log.warning(s"Unhandled message in Player-$username: $unhandled")
  }

  private def handleInteractionOn(block: Blocks.Block): Unit = block.name match {
    case "crafting_table" => lastWindowId = openCraftingTableWindow()
    case _ => //not implemented
  }

  private def playerPositionAndLookFlags(xRelative: Boolean, yRelative: Boolean, zRelative: Boolean, Y_ROT: Boolean,
                                         X_ROT: Boolean): Byte = {
    var flags: Byte = 0x00
    if (xRelative) flags = (flags | 0x01).toByte
    if (yRelative) flags = (flags | 0x02).toByte
    if (zRelative) flags = (flags | 0x04).toByte
    if (Y_ROT) flags = (flags | 0x08).toByte
    if (X_ROT) flags = (flags | 0x10).toByte

    flags
  }

  private def updatePositionAndLook(coordinates: Option[(Double, Double, Double)] = None,
                                    direction: Option[(Float, Float)] = None, onGround: Boolean = true): Unit = {

    def relative(current: Double, previous: Double): Int = ((current * 32 - previous * 32) * 128).toInt

    val relatives = coordinates map { case (x, y, z) => (relative(x, posX), relative(y, posY), relative(z, posZ)) }
    val packets = (relatives, direction) match {
      case (Some((x, y, z)), None) => List(cb.EntityRelativeMove(playerEntityId, x, y, z, onGround))
      case (None, Some((yaw, pitch))) => List(
        cb.EntityLook(playerEntityId, Angle.fromFloatYaw(yaw), Angle.fromFloatPitch(pitch), onGround),
        cb.EntityHeadLook(playerEntityId, Angle.fromFloatYaw(yaw))
      )
      case (Some((x, y, z)), Some((yaw, pitch))) => List(
        cb.EntityLookAndRelativeMove(playerEntityId, x, y, z, Angle.fromFloatYaw(yaw), Angle.fromFloatPitch(pitch), onGround),
        cb.EntityHeadLook(playerEntityId, Angle.fromFloatYaw(yaw))
      )
      case (None, None) => List(
        cb.EntityLook(playerEntityId, Angle.fromFloatYaw(yaw), Angle.fromFloatPitch(pitch), onGround)
      )
    }

    coordinates foreach { case (x, y, z) =>
      this.posX = x
      this.posY = y
      this.posZ = z
      world ! PlayerMoved(playerEntityId, Position(x, y, z))
      loadChunksAndMobs()
    }

    direction foreach { case (yaw, pitch) =>
      this.yaw = yaw
      this.pitch = pitch
    }

    this.onGround = onGround

    packets foreach { packet =>
      world ! SendToAllExclude(playerEntityId, packet)
    }
  }

  private def openCraftingTableWindow(): Int = {
    val windowId = lastWindowId + 1
    val playerInventoryActorRef = activeInventories(PlayerInventory.Id)
    activeInventories += (windowId -> context.actorOf(CraftingTableActor.props(windowId, self, playerInventoryActorRef)))
    (activeInventories(PlayerInventory.Id) ? RetrieveInventoryItems).mapTo[List[Option[InventoryItem]]] onComplete {
      case Success(inventory) => activeInventories(windowId) ! PopulatePlayerInventory(inventory)
      case _ => log.warning("Failed to retrieve inventory items.")
    }
    userContext ! OpenWindows(windowId, CraftingTable("{\"translate\":\"block.minecraft.crafting_table.name\"}", 0))
    windowId
  }

  private def reset(): Unit = {
    loadedChunks = Set()
    lastPosition = null
    keepAliveCancellable.cancel()
    context.become(preStartBehaviour)
  }

  private var lastPosition: (Double, Double) = _

  private def loadChunksAndMobs(): Future[Unit] = {
    val timeout = 30 seconds

    def needLoadingChunks: Boolean =
      math.abs(posX - lastPosition._1) > ServerConfiguration.LoadingChunksBlocksThreshold ||
        math.abs(posZ - lastPosition._2) > ServerConfiguration.LoadingChunksBlocksThreshold

    def loadChunks(toUnload: Set[(Int, Int)], toLoad: Set[(Int, Int)]): Future[Unit] = {
      toUnload foreach { case (x, z) => userContext ! UnloadChunk(x, z) }
      Future.sequence(toLoad map { case (x, z) =>
        world.ask(RequestChunkData(x, z))(timeout) map {
          case chunkData: ChunkData => userContext ! chunkData
          case _ => // do nothing
        }
      }) map (_ => Unit)
    }

    def loadMobs(toUnload: Set[(Int, Int)], toLoad: Set[(Int, Int)]): Future[Unit] = {
      val unloadFuture = Future.sequence(toUnload map { case (x, z) =>
        world.ask(PlayerUnloadedChunk(x, z))(timeout).mapTo[List[DestroyEntities]] map (destroyCreatures =>
          destroyCreatures.foreach(destroyCreature => userContext ! destroyCreature))
      }) map (_ => Unit)
      val loadFuture = Future.sequence(toLoad map { case (x, z) =>
        world.ask(SpawnCreaturesInChunk(x, z))(timeout).mapTo[List[SpawnMob]] map (spawnMobs =>
          spawnMobs.foreach(spawnMob => userContext ! spawnMob))
      }) map (_ => Unit)
      loadFuture zip unloadFuture flatMap (_ => Future.unit)
    }

    if (lastPosition == null || needLoadingChunks) {
      val chunkX = posX.toInt >> 4
      val chunkZ = posZ.toInt >> 4
      lastPosition = (posX, posZ)
      val newChunks = (for (x <- chunkX - viewDistance to chunkX + viewDistance;
                            z <- chunkZ - viewDistance to chunkZ + viewDistance) yield (x, z)).toSet
      val toUnload = loadedChunks diff newChunks
      val toLoad = newChunks diff loadedChunks
      loadedChunks = newChunks

      val loadChunksFuture = loadChunks(toUnload, toLoad)
      val loadMobFuture = loadMobs(toUnload, toLoad)
      loadChunksFuture zip loadMobFuture flatMap (_ => Future.unit)
    } else Future.unit
  }

  private def spawnPlayerPacket = cb.SpawnPlayer(playerEntityId, playerUUID, posX, posY, posZ,
    Angle.fromFloatYaw(yaw), Angle.fromFloatPitch(pitch), entityMetadata)

}

object Player {

  import AttributeModifier._

  private val PlayerEntityProperties = List(
    Property(GenericMaxHealth, 20f, List()),
    Property(MovementSpeed, 0.1f, List()),
    Property(AttackSpeed, 4.0f, List()),
    Property(Armor, 0.0f, List()),
    Property(ArmorToughness, 0.0f, List()),
    Property(GenericLuck, 0.0f, List())
  )

  def props(username: String, playerUUID: UUID, serverConfiguration: ServerConfiguration): Props =
    Props(new Player(username, playerUUID: UUID, serverConfiguration))

  def name(username: String): String = s"Player-$username"

}
