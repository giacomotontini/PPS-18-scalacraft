package io.scalacraft.logic.creatures.behaviours

import akka.pattern._
import io.scalacraft.logic.commons.Message.{RequestNearbyPoints, SendToAll}
import io.scalacraft.logic.commons.Traits.EnrichedActor
import io.scalacraft.logic.creatures.parameters.CreatureParameters
import io.scalacraft.packets.DataTypes.{Angle, Position}
import io.scalacraft.packets.Entities.MobEntity
import io.scalacraft.packets.clientbound.PlayPackets.{EntityHeadLook, EntityLookAndRelativeMove, EntityVelocity}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

trait Movement[T <: MobEntity] {
  this: CreatureParameters[T] with EnrichedActor =>

  import Movement._
  val MovementTickPeriod: FiniteDuration = 10 seconds

  protected case object MoveEntity

  case class Move(deltaX: Int, deltaY: Int, deltaZ: Int, newPosition: Position)

  def computeMoves(posX: Int, posY: Int, posZ: Int, oldPosX: Int, oldPosZ: Int, movesNumber: Int): Future[List[Move]] = {

    def computeMove(nearbyPositions: List[Position]): Move = {
      val newPosition = nearbyPositions(randomGenerator.nextInt(nearbyPositions.size))
      val deltaX = (newPosition.x * 32 - posX * 32) * 128
      val deltaY = (newPosition.y * 32 - posY * 32) * 128
      val deltaZ = (newPosition.z * 32 - posZ * 32) * 128
      Move(deltaX, deltaY, deltaZ, newPosition)
    }

    if (movesNumber > 0) {
      (world ? RequestNearbyPoints(posX, posY, posZ, oldPosX, oldPosZ)).mapTo[List[Position]] flatMap {
        case nearbyPoints if nearbyPoints.nonEmpty =>
          val move = computeMove(nearbyPoints)
          val newPosition = move.newPosition
          computeMoves(newPosition.x, newPosition.y, newPosition.z, posX, posZ, movesNumber - 1) map {
            moves => move :: moves
          }
      }
    } else Future.successful(List())
  }

  def doMove(): Unit = {

    def fluidMovementAnimation(move: Move, moveIndex: Int): Unit = {

      def obtainFluidVelocity(newPosition: Position): (Int, Int, Int) = {
        val velocityX = speed * newPosition.x / MovementFluidityFactor
        val velocityY = speed * newPosition.y / MovementFluidityFactor
        val velocityZ = speed * newPosition.z / MovementFluidityFactor
        (velocityX, velocityY, velocityZ)
      }

      for (scheduleFactor <- 1 to MovementFluidityFactor) {
        val schedulePeriod = (millisPerChunkOfBlock * scheduleFactor + millisPerBlock * moveIndex).toLong
        context.system.scheduler.scheduleOnce(FiniteDuration(schedulePeriod, "millisecond")) {
          val (deltaXChunk, deltaYChunk, deltaZChunk) = (move.deltaX / MovementFluidityFactor,
            move.deltaY / MovementFluidityFactor, move.deltaZ / MovementFluidityFactor)
          val (yaw, pitch) = computeYawAndPitch(deltaXChunk, deltaYChunk, deltaZChunk)
          val (velocityX, velocityY, velocityZ) = obtainFluidVelocity(move.newPosition)
          world ! SendToAll(EntityVelocity(entityId, velocityX, velocityY, velocityZ))
          world ! SendToAll(EntityHeadLook(entityId, yaw))
          world ! SendToAll(EntityLookAndRelativeMove(entityId, deltaXChunk, deltaYChunk, deltaZChunk, yaw, pitch,
            deltaYChunk == 0))
        }
      }
    }

    computeMoves(posX, posY, posZ, oldPosX, oldPosZ, pathMovesNumber) onComplete {
      case Success(moves) =>
        moves.foreach { move =>
          fluidMovementAnimation(move, moves.indexOf(move))
        }
        val newPosition = moves.reverse.head.newPosition
        posX = newPosition.x
        posY = newPosition.y
        posZ = newPosition.z

      case Failure(_) => // do nothing
    }
  }
}

object Movement {

  type YawAndPitch = (Angle, Angle)

  private val SquareExponent = 2
  private val HalfCircumferenceMap = 128
  private val CircumferenceMap = 256

  def computeYawAndPitch(deltaX: Int, deltaY: Int, deltaZ: Int): YawAndPitch = {
    val radius = Math.sqrt(Math.pow(deltaX, SquareExponent) + Math.pow(deltaY, SquareExponent) +
      Math.pow(deltaZ, SquareExponent))
    val newYaw = {
      val _yaw = -Math.atan2(deltaX, deltaZ) / Math.PI * HalfCircumferenceMap
      _yaw + (if (_yaw < 0) CircumferenceMap else 0)
    }
    val newPitch = -Math.asin(deltaY / radius) / Math.PI * HalfCircumferenceMap
    (Angle(newYaw.toInt), Angle(newPitch.toInt))
  }

}