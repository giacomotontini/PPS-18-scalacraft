package io.scalacraft.logic

import akka.actor.Props
import akka.pattern._
import io.scalacraft.loaders.{Blocks, Chunks}
import io.scalacraft.logic.commons.Message.{RequestChunkData, RequestNearbyPoints, RequestSpawnPoints, _}
import io.scalacraft.logic.commons.RichNbtCompoundTag._
import io.scalacraft.logic.commons.Traits.EnrichedActor
import io.scalacraft.logic.creatures.ComputeCreatureMoves
import io.scalacraft.logic.creatures.Spawnables.PositionWithProperties
import io.scalacraft.misc.Helpers
import io.scalacraft.misc.Helpers._
import io.scalacraft.packets.DataTypes.Position
import io.scalacraft.packets.clientbound.PlayPackets.ChunkData
import net.querz.nbt.CompoundTag
import net.querz.nbt.mca.{Chunk, MCAFile}

import scala.concurrent.Future
import scala.util.{Failure, Success}


/**
 * Actor that represent a region in the world. Replies at the requests of chunk data, set/get the state or light of
 * a block, find a block under some conditions.
 *
 * @param mca the in-memory file that contains all the properties and data of the region
 */
class Region(mca: MCAFile) extends EnrichedActor {

  import Region._

  private val world = context.parent

  private var cleanupNeeded: Boolean = false

  private[this] def firstSpawnableHeight(chunk: Chunk, x: Int, z: Int): Int = {
    var yIndex = 255
    while (chunk.getBlockStateAt(x, yIndex, z) == null || !chunk.getBlockStateAt(x, yIndex, z).isSpawnableSurface) {
      yIndex -= 1
    }
    yIndex + 1
  }

  private[this] def cubeStates(chunk: Chunk, blockX: Int, blockY: Int, blockZ: Int): Seq[String] =
    for (yDrift <- -2 to 1) yield {
      val nbtBlockState = chunk.getBlockStateAt(blockX, blockY + yDrift, blockZ)
      val blockState = if (nbtBlockState.isSpawnableSurface) "surface"
      else if (nbtBlockState.emptyCube) "noSurface" else "unused"
      s"state($blockX,${blockY + yDrift},$blockZ,$blockState)"
    }

  override def receive: Receive = {

    case RequestChunkData(chunkX, chunkZ, fullChunk) =>
      val chunk = mca.getChunk(chunkX, chunkZ)
      if (cleanupNeeded) chunk.cleanupPalettesAndBlockStates()

      sender ! (if (chunk == null) ChunkNotPresent else {
        val (data, primaryBitMask) = Chunks.buildChunkDataStructureAndBitmask(chunk)
        val entities = Helpers.listTagToList(chunk.getEntities)

        ChunkData(chunkX, chunkZ, fullChunk, primaryBitMask, data, entities)
      })

    case RequestBlockState(Position(x, y, z)) => sender ! mca.getChunk(x >> 4, z >> 4).getBlockStateAt(x, y, z)

    case RequestLight(Position(x, y, z)) =>
      val LightPosition(index, shifts) = lightIndex(x, y, z)
      val section = mca.getChunk(x >> 4, z >> 4).getSection(y / 16)
      sender ! Light((section.getBlockLight()(index) >> shifts & 0xF).toByte,
        (section.getSkyLight()(index) >> shifts & 0xF).toByte)

    case ChangeBlockState(Position(x, y, z), tag) =>
      mca.getChunk(x >> 4, z >> 4).setBlockStateAt(x, y, z, tag, false)
      // calculateLights(position, Light(-1, -1), tag)
      cleanupNeeded = true

    case ChangeLight(Position(x, y, z), Light(blockLight, skyLight)) =>
      val section = mca.getChunk(x >> 4, z >> 4).getSection(y / 16)
      val LightPosition(index, shifts) = lightIndex(x, y, z)
      val mask = 0xF0 >> shifts

      var newBlockLight = section.getBlockLight()(index).toInt
      newBlockLight &= mask
      newBlockLight |= blockLight << shifts
      section.getBlockLight()(index) = newBlockLight.toByte
      var newSkyLight = section.getSkyLight()(index).toInt
      newSkyLight &= mask
      newSkyLight |= skyLight << shifts
      section.getSkyLight()(index) = newSkyLight.toByte

    case FindFirstSolidBlockPositionUnder(Position(x, y, z)) =>
      val chunk = mca.getChunk(x >> 4, z >> 4)
      var currentY = y
      while (chunk.getBlockStateAt(x, currentY, z).emptyCube) currentY -= 1
      sender ! Position(x, currentY, z)

    case RequestSpawnPoints(chunkX, chunkZ) =>
      val chunkColumn = mca.getChunk(chunkX, chunkZ)
      val biomeToSpawnPosition = (for (x <- 0 to 15;
                                       z <- 0 to 15) yield {
        val posX = (chunkX << 4) + x
        val posZ = (chunkZ << 4) + z
        val posY = firstSpawnableHeight(chunkColumn, x, z)
        val biome = chunkColumn.getBiomeAt(x, z)
        val isWater = chunkColumn.getBlockStateAt(x, posY - 1, z).isWater
        biome -> PositionWithProperties(Position(posX, posY, posZ), isWater)
      }) groupBy (_._1) map {
        case (biomeIndex, values) => biomeIndex -> (values map (_._2)).toSet
      }
      sender ! biomeToSpawnPosition

    case RequestNearbyPoints(x: Int, y: Int, z: Int, oldX: Int, oldZ: Int) =>
      val chunk = mca.getChunk(x >> 4, z >> 4)
      val forbiddenXZPair = (oldX - x, oldZ - z)
      var cubeStatesAssertions = Seq[String]()
      for (xzPair <- RelativeHorizontalNears if xzPair != forbiddenXZPair) {
        cubeStatesAssertions ++= cubeStates(chunk, x + xzPair._1, y, z + xzPair._2)
      }
      val computeCreaturesMoves = new ComputeCreatureMoves(cubeStatesAssertions)
      sender ! computeCreaturesMoves.computeMoves(x, y, z)

    case unhandled => log.warning(s"Unhandled message in Region: $unhandled")
  }

  private def lightIndex(blockX: Int, blockY: Int, blockZ: Int): LightPosition =
    LightPosition(((blockY & 0xF) * 256 + (blockZ & 0xF) * 16 + (blockX & 0xF)) / 2, (blockX & 0xF % 2) * 4)

  /**
   * Calculate the lights of blocks and sky from an initial position.
   * WARNING: this method is broken because there are concurrent problems when asking other regions block lights.
   */
  private def calculateLights(position: Position, light: Light, tag: CompoundTag): Unit = {
    val block = Blocks.blockFromCompoundTag(tag)

    Future.sequence(for (Position(rx, ry, rz) <- RelativeNears) yield {
      val pos = Position(position.x + rx, position.y + ry, position.z + rz)
      (world ? RequestLight(pos) zip world ? RequestBlockState(pos)).mapTo[(Light, CompoundTag)] map {
        case (light, _tag) => TagLightAndPosition(light, _tag, pos)
      }
    }) onComplete {
      case Success(bottom :: top :: north :: south :: west :: east :: Nil) =>
        val all = List(bottom, top, north, south, west, east)
        val newLight = Light(
          (if (block.transparent) {
            math.max((all maxBy (_.light.blockLight)).light.blockLight - 1, block.light)
          } else block.light).toByte,
          (if (block.transparent) {
            if (top.light.skyLight == MaxLight) MaxLight
            else math.max((all maxBy (_.light.skyLight)).light.skyLight - 1, MinLight)
          } else MinLight).toByte
        )

        if (light != newLight) {
          world ! ChangeLight(position, newLight)

          for (direction <- all) {
            calculateLights(direction.position, direction.light, direction.tag)
          }
        }
      case Success(_) => // never happens
      case Failure(ex) => log.error(ex, "Failed to retrieve lights")
    }
  }

}

object Region {

  private val RelativeHorizontalNears = List((-1, 0), (1, 0), (0, 1), (0, -1))

  /**
   * Represent the light of a block, composed by the block light and the sky light
   *
   * @param blockLight the level of the block light
   * @param skyLight the level of the sky light
   */
  case class Light(blockLight: Byte, skyLight: Byte)

  private case class LightPosition(index: Int, shifts: Int)

  private case class TagLightAndPosition(light: Light, tag: CompoundTag, position: Position)

  private val MaxLight: Byte = 15
  private val MinLight: Byte = 0

  def props(mca: MCAFile): Props = Props(new Region(mca))

  def name(x: Int, z: Int): String = s"region$x,$z"

}
