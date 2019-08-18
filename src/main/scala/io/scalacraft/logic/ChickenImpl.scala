package io.scalacraft.logic

import java.util.UUID

import akka.actor.{Actor, ActorRef, Props, Timers}
import io.scalacraft.core.marshalling.MobsAndObjectsTypeMapping
import io.scalacraft.logic.ChickenImpl.MoveEntity
import io.scalacraft.logic.messages.Message.{AskResponse, DespawnCreature, GetCreatureInChunk, RequestNearbyPoints}
import io.scalacraft.logic.traits.{DefaultTimeout, ImplicitContext}
import io.scalacraft.logic.traits.creatures.FarmAnimal
import io.scalacraft.packets.DataTypes.{Angle, Position}
import io.scalacraft.packets.Entities.Chicken
import io.scalacraft.packets.clientbound.PlayPackets.{DestroyEntities, EntityRelativeMove, SpawnMob}
import net.querz.nbt.mca.MCAUtil

import scala.concurrent.duration._

class ChickenImpl(entityId: Int, UUID: UUID, x: Int, y: Int, z: Int, isBaby: Boolean, world: ActorRef)
  extends Actor with Timers with ImplicitContext with DefaultTimeout {
  val metaData = new Chicken()
  metaData.isBaby = isBaby
  private val tpe = MobsAndObjectsTypeMapping.fromMobEntityClassToType(metaData.getClass)
  private val velX, velZ = 2000
  private val velY = 0
  private val yaw = Angle(0);
  private val pitch = Angle(0);
  private val headPitch = Angle(0)
  private var posX = x
  private var posY = y
  private var posZ = z

  private val movementTickPeriod = 2 second

  private[this] def isMyChunk(chunkX: Int, chunkZ: Int): Boolean = {
    MCAUtil.blockToChunk(x) == chunkX && MCAUtil.blockToChunk(z) == chunkZ
  }

  override def preStart(): Unit = {
    timers.startPeriodicTimer(new Object(), MoveEntity, movementTickPeriod)
  }

  private def computeYawAndPitch(deltaX: Int, deltaY: Int, deltaZ: Int): (Angle, Angle) = {
    val squareExponent = 2
    val halfCircumferenceMap = 128
    val circumferenceMap = 256
    val radius = Math.sqrt(Math.pow(deltaX, squareExponent) +
      Math.pow(deltaY, squareExponent) +
      Math.pow(deltaZ, squareExponent))
    val yaw = -Math.atan2(deltaX, deltaZ) / Math.PI * halfCircumferenceMap match {
      case temporaryYaw if temporaryYaw < 0 => circumferenceMap + temporaryYaw
      case temporaryYaw => temporaryYaw
    }
    val pitch = -Math.asin(deltaY / radius) / Math.PI * halfCircumferenceMap
    (Angle(yaw.toInt), Angle(pitch.toInt))
  }

  private def getDeltaMovementOnXYZ(): List[Position] = {
    world ? RequestNearbyPoints(posX,posY, posZ)
  }

  override def receive: Receive = {
    case GetCreatureInChunk(chunkX, chunkZ) =>
      if (isMyChunk(chunkX, chunkZ))
        sender ! AskResponse(self, Some(SpawnMob(entityId, UUID, tpe, x, y, z, yaw, pitch, headPitch, velX, velY, velZ, metaData)))
      else sender ! AskResponse(self, None)
    case DespawnCreature(chunkX, chunkZ) =>
      if (isMyChunk(chunkX, chunkZ)) {
        sender ! AskResponse(self, Some(DestroyEntities(List(entityId))))
      }
      else sender ! AskResponse(self, None)
    case MoveEntity =>
      val blockOfMovementOnXAxis = velX/8000*movementTickPeriod/50
      val blockOfMovementOnZAxis = velZ/8000*movementTickPeriod/50
      val currentX = posX + 2
      //val currentY = posY + 1
      val currentZ = posZ + 2
      val deltaX = (currentX * 32 - posX * 32) * 128
      //val deltaY = (currentY * 32 - posY * 32) * 128
      val deltaZ = (currentZ * 32 - posZ * 32) * 128
      posX = currentX;
      //posY = currentY;
      posZ = currentZ

      world ! EntityRelativeMove(entityId, deltaX, 0, deltaZ, onGround = true)
  }
}

object ChickenImpl extends FarmAnimal {

  private case object MoveEntity

  override def props(entityId: Int, UUID: UUID, x: Int, y: Int, z: Int, isBaby: Boolean, world: ActorRef): Props =
    Props(new ChickenImpl(entityId, UUID, x, y, z, isBaby, world))

  override def name(UUID: UUID): String = s"Chicken-$UUID"
}