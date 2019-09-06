package io.scalacraft.logic

import akka.actor.{ActorRef, PoisonPill, Props}
import akka.pattern._
import io.scalacraft.core.packets.clientbound.PlayPackets.SpawnMob
import io.scalacraft.logic.commons.Message
import io.scalacraft.logic.commons.Message.SkyUpdateState.SkyUpdateState
import io.scalacraft.logic.commons.Message._
import io.scalacraft.logic.commons.Traits.EnrichedActor
import io.scalacraft.logic.creatures.CreatureInstances
import io.scalacraft.logic.creatures.Spawnables.{PositionWithProperties, SpawnableFarmAnimal}

import scala.concurrent.Future
import scala.concurrent.Future._
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
 * The actor that handle the spawn and despawn of creatures based on the player position and the sky state.
 */
class CreatureSpawner extends EnrichedActor {

  type ChunkLocation = (Int, Int)

  /**
   * Mantains the number of player in a chunk, this is identified by his x, z coordinates
   */

  var numberOfPlayersInChunk: Map[ChunkLocation, Int] = Map()

  /**
   * Indicates if creatures have been spawned yet in a given chunk
   */

  var spawnedMobFuturePerChunk: Map[ChunkLocation, Future[_]] = Map()
  var spawnedActor: Set[ActorRef] = Set()
  val timeout: FiniteDuration = 20 seconds
  var isDay: Boolean = _

  /**
   * Update numberOfPlayer in a chunk and get the actual number of player.
   * @param chunkX the x coordinate of the chunk
   * @param chunkZ the z coordinate of the chunk
   * @param updateFunction the update function coordinate of the chunk
   * @param removeEntryPredicate the condition which establioshes if the entry must be removed
   * @return the updated map and the actual number of player in the chunk
   */
  private[this] def updateNumberOfPlayerInChunk(chunkX: Int, chunkZ: Int,
                                                updateFunction: Int => Int, removeEntryPredicate: Int => Boolean):
  (Map[ChunkLocation, Int], Int) = {
    val parameterToBeUpdated = numberOfPlayersInChunk.getOrElse((chunkX, chunkZ), 0)
    val updatedParameter = updateFunction(parameterToBeUpdated)
    val updatedMap = if (removeEntryPredicate(updatedParameter)) numberOfPlayersInChunk - ((chunkX, chunkZ))
    else numberOfPlayersInChunk.updated((chunkX, chunkZ), updatedParameter)
    (updatedMap, updatedParameter)
  }

  private[this] def askSomethingToCreatures[T](request: Message, onResult: List[T] => Unit,
                                               removeActor: Boolean = false): Unit = {
    sequence(spawnedActor.map(actor => actor ? request).toList) onComplete {
      case Success(mobResponse) =>
        onResult(mobResponse collect {
          case AskResponse(senderRef, Some(response)) =>
            if (removeActor) {
              senderRef ! PoisonPill
              spawnedActor -= senderRef
            }
            response.asInstanceOf[T]
        })
      case Failure(ex) => log.error(ex, "Request failed at askSomethingToCreatures")
    }
  }

  override def receive: Receive = {

    case SpawnCreaturesInChunk(chunkX, chunkZ) =>
      val senderRef = sender
      val (updatedMap, _) = updateNumberOfPlayerInChunk(chunkX = chunkX, chunkZ = chunkZ, updateFunction = _ + 1,
        removeEntryPredicate = _ => false)
      numberOfPlayersInChunk = updatedMap
      if (!spawnedMobFuturePerChunk.contains(chunkX, chunkZ)) {
        val spawnedMobFuture = context.parent.ask(RequestSpawnPoints(chunkX, chunkZ))(timeout)
          .mapTo[Map[Int, Set[PositionWithProperties]]].andThen {
          case Success(biomeToSpawnPosition) =>
            var spawnablePosition = biomeToSpawnPosition
            CreatureInstances.creatureInstances foreach {
              case farmAnimal: SpawnableFarmAnimal if isDay =>
                val spawnResult = farmAnimal.spawnFarmAnimal(spawnablePosition, context.parent)
                spawnablePosition = spawnResult.updatedPosition
                spawnedActor ++= spawnResult.actorToSpawn.map {
                  case (props: Props, name: String) => context.actorOf(props, name)
                }
              case _ => // none
            }
            askSomethingToCreatures[SpawnMob](RequestCreatureInChunk(chunkX, chunkZ), senderRef ! _)
        }
        spawnedMobFuturePerChunk ++= Map((chunkX, chunkZ) -> spawnedMobFuture)
      } else {
        spawnedMobFuturePerChunk((chunkX, chunkZ)).onComplete {
          case Success(_) => askSomethingToCreatures[SpawnMob](RequestCreatureInChunk(chunkX, chunkZ), senderRef ! _)
          case _ => //do nothing
        }
      }

    case PlayerUnloadedChunk(chunkX, chunkZ) =>
      val senderRef = sender
      val (updatedMap, actualNumberOfPlayer) = updateNumberOfPlayerInChunk(chunkX = chunkX, chunkZ = chunkZ,
        updateFunction = _ - 1, removeEntryPredicate = _ == 0)
      numberOfPlayersInChunk = updatedMap
      if (spawnedMobFuturePerChunk.contains(chunkX, chunkZ) && actualNumberOfPlayer == 0) {
        askSomethingToCreatures[DespawnCreature](DespawnCreature(chunkX, chunkZ), senderRef ! _)
        spawnedMobFuturePerChunk = spawnedMobFuturePerChunk - ((chunkX, chunkZ))
      }

    case SkyStateUpdate(state: SkyUpdateState) => state match {
      case SkyUpdateState.Sunrise | SkyUpdateState.Noon => isDay = true
      case SkyUpdateState.Sunset | SkyUpdateState.MidNight => isDay = false
    }

    case useEntityWithItem: UseEntityWithItem => spawnedActor foreach (_.forward(useEntityWithItem))

    case entityDead: EntityDead =>
      context.parent forward entityDead
      spawnedActor -= sender
      sender ! PoisonPill
  }
}

object CreatureSpawner {

  def props: Props = Props(new CreatureSpawner)

  def name: String = "creatureSpawner"

}
