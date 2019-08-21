package io.scalacraft.logic.traits.ai.general

import akka.actor.{Actor, Timers}
import io.scalacraft.logic.messages.Message.RequestNearbyPoints
import io.scalacraft.logic.traits.{DefaultTimeout, ImplicitContext}
import io.scalacraft.packets.DataTypes.{Angle, Position}
import io.scalacraft.packets.clientbound.PlayPackets.{EntityHeadLook, EntityLockAndRelativeMove, EntityVelocity}

import scala.concurrent.Future
import scala.concurrent.duration._
import akka.pattern._
import io.scalacraft.logic.traits.creatures.CreatureParameters

import scala.util.{Failure, Success}

trait Movement {
  this: CreatureParameters with Actor with Timers with ImplicitContext with DefaultTimeout =>
  private val SquareExponent = 2
  private val HalfCircumferenceMap = 128
  private val CircumferenceMap = 256
  val MovementTickPeriod: FiniteDuration = 10 seconds
  val AiTimerKey = "AiMovement"

  protected case object MoveEntity

  case class Move(deltaX: Int, deltaY: Int, deltaZ: Int, newPosition: Position)

  def computeAndUpdateYawAndPitch(deltaX: Int, deltaY: Int, deltaZ: Int): (Angle, Angle) = {
    val radius = Math.sqrt(Math.pow(deltaX, SquareExponent) +
      Math.pow(deltaY, SquareExponent) +
      Math.pow(deltaZ, SquareExponent))
    val newYaw = -Math.atan2(deltaX, deltaZ) / Math.PI * HalfCircumferenceMap match {
      case temporaryYaw if temporaryYaw < 0 => CircumferenceMap + temporaryYaw
      case temporaryYaw => temporaryYaw
    }
    val newPitch = -Math.asin(deltaY / radius) / Math.PI * HalfCircumferenceMap
    (Angle(newYaw.toInt), Angle(newPitch.toInt))
  }

  def computeMoves(posX: Int, posY: Int, posZ: Int, oldPosX: Int, oldPosZ: Int, movesNumber: Int): Future[List[Move]] = {

    def computeMove(nearbyPositions: List[Position]): Move = {
      val newPosition = nearbyPositions(randomGenerator.nextInt(nearbyPositions.size))
      val deltaX = (newPosition.x * 32 - posX * 32) * 128
      val deltaY = (newPosition.y * 32 - posY * 32) * 128
      val deltaZ = (newPosition.z * 32 - posZ * 32) * 128
      Move(deltaX, deltaY, deltaZ, newPosition)
    }

    movesNumber match {
      case _ if movesNumber > 0 =>
        (world ? RequestNearbyPoints(posX, posY, posZ, oldPosX, oldPosZ)).mapTo[List[Position]] flatMap {
          case nearbyPoints if nearbyPoints.nonEmpty =>
            val move = computeMove(nearbyPoints)
            val newPosition = move.newPosition
            computeMoves(newPosition.x, newPosition.y, newPosition.z, posX, posZ, movesNumber - 1) map {
              moves => move :: moves
            }
        }
      case _ =>
        Future.successful(List())
    }
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
          val (deltaXChunk, deltaYChunk, deltaZChunk) =
            (move.deltaX / MovementFluidityFactor, move.deltaY / MovementFluidityFactor, move.deltaZ / MovementFluidityFactor)
          val (yaw, pitch) = computeAndUpdateYawAndPitch(deltaXChunk, deltaYChunk, deltaZChunk)
          val (velocityX, velocityY, velocityZ) = obtainFluidVelocity(move.newPosition)
          world ! EntityVelocity(entityId, velocityX, velocityY, velocityZ)
          world ! EntityHeadLook(entityId, yaw)
          world ! EntityLockAndRelativeMove(entityId, deltaXChunk, deltaYChunk, deltaZChunk, yaw, pitch, deltaYChunk == 0)
        }
      }
    }

    computeMoves(posX, posY, posZ, oldPosX, oldPosZ, pathMovesNumber).onComplete {
      case Success(moves) =>
        moves.foreach { move =>
          fluidMovementAnimation(move, moves.indexOf(move))
        }
        val newPosition = moves.reverse.head.newPosition
        posX = newPosition.x
        posY = newPosition.y
        posZ = newPosition.z
        world ! EntityHeadLook(entityId, Angle(0))
        timers.startPeriodicTimer(AiTimerKey, MoveEntity, MovementTickPeriod)
      case Failure(_) => //do nothing
    }
  }
}
