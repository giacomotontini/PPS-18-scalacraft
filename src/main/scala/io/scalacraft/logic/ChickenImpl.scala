package io.scalacraft.logic

import java.util.UUID

import scala.util.{Failure, Random, Success}
import akka.pattern._
import akka.actor.{Actor, ActorRef, Props, Timers}
import io.scalacraft.core.marshalling.MobsAndObjectsTypeMapping
import io.scalacraft.logic.ChickenImpl.MoveEntity
import io.scalacraft.logic.messages.Message.{AskResponse, DespawnCreature, GetCreatureInChunk, RequestNearbyPoints}
import io.scalacraft.logic.traits.{DefaultTimeout, ImplicitContext}
import io.scalacraft.logic.traits.creatures.FarmAnimal
import io.scalacraft.packets.DataTypes.{Angle, Position}
import io.scalacraft.packets.Entities.Chicken
import io.scalacraft.packets.clientbound.PlayPackets.{DestroyEntities, EntityHeadLook, EntityLockAndRelativeMove, EntityLook, EntityRelativeMove, EntityVelocity, SpawnMob}
import net.querz.nbt.mca.MCAUtil

import scala.concurrent.duration._
import scala.util.Random

class ChickenImpl(entityId: Int, UUID: UUID, x: Int, y: Int, z: Int, isBaby: Boolean, world: ActorRef)
  extends Actor with Timers with ImplicitContext with DefaultTimeout {
  val metaData = new Chicken()
  metaData.isBaby = isBaby
  private val tpe = MobsAndObjectsTypeMapping.fromMobEntityClassToType(metaData.getClass)
  private val velX, velZ = 4000
  private val velY = 0
  private var yaw = Angle(0);
  private var pitch = Angle(0);
  private val headPitch = Angle(0)
  private var posX = x
  private var posY = y
  private var posZ = z
  private var oldPosX = x
  private var oldPosY = y
  private var oldPosZ = z
  val randomGenerator: Random.type = scala.util.Random

  private val movementTickPeriod = 5 seconds

  private[this] def isMyChunk(chunkX: Int, chunkZ: Int): Boolean = {
    MCAUtil.blockToChunk(x) == chunkX && MCAUtil.blockToChunk(z) == chunkZ
  }

  override def preStart(): Unit = {
    timers.startPeriodicTimer(new Object(), MoveEntity, movementTickPeriod)
  }

  private def computeYawAndPitch(deltaX: Int, deltaY: Int, deltaZ: Int): Unit = {
    val squareExponent = 2
    val halfCircumferenceMap = 128
    val circumferenceMap = 256
    val radius = Math.sqrt(Math.pow(deltaX, squareExponent) +
      Math.pow(deltaY, squareExponent) +
      Math.pow(deltaZ, squareExponent))
    val newYaw = -Math.atan2(deltaX, deltaZ) / Math.PI * halfCircumferenceMap match {
      case temporaryYaw if temporaryYaw < 0 => circumferenceMap + temporaryYaw
      case temporaryYaw => temporaryYaw
    }
    val newPitch = -Math.asin(deltaY / radius) / Math.PI * halfCircumferenceMap
    yaw = Angle(newYaw.toInt)
    pitch = Angle(newPitch.toInt)
  }

  private def computeAndDoMove(): Unit = {
    def doMove(newPosition: Position): Unit = {
      val deltaX = (newPosition.x * 32 - posX * 32) * 128
      val deltaY = (newPosition.y * 32 - posY * 32) * 128
      val deltaZ = (newPosition.z * 32 - posZ * 32) * 128
      //println("actual_pos: " + (posX, posY, posZ) +" new_pos "+ (newPosition.x, newPosition.y, newPosition.z))
      oldPosX = posX
      oldPosY = posY
      oldPosZ = posZ
      posX = newPosition.x
      posY = newPosition.y
      posZ = newPosition.z
      //posZ -= 1
      computeYawAndPitch(deltaX, deltaY, deltaZ)
      //world ! EntityVelocity(entityId, velX, velY, velZ)
      //world ! EntityLook(entityId, yaw, pitch, false)
      world ! EntityVelocity(entityId, velX, velY, velZ)
      world ! EntityHeadLook(entityId, yaw)
      for(i <- 1 to 4) {
        context.system.scheduler.scheduleOnce(i * 50 millisecond){
          world ! EntityLockAndRelativeMove(entityId, deltaX/4, deltaY/4, deltaZ/4, yaw, pitch, deltaY==0)
        }
      }
      //world ! EntityVelocity(entityId, velX, velY, velZ)
    }
    for(i <- 1 to 8) {
      context.system.scheduler.scheduleOnce(i * 250 millisecond) {
        world.ask(RequestNearbyPoints(posX, posY, posZ, oldPosX, oldPosZ))(timeout = 10000 millisecond) onComplete {
          case Success(nearbyPoints: List[Position]) =>
            if (nearbyPoints.nonEmpty) {
              doMove(nearbyPoints(randomGenerator.nextInt(nearbyPoints.size)))
            }
        }
      }
    }
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
      computeAndDoMove()

  }
}

object ChickenImpl extends FarmAnimal {

  private case object MoveEntity

  override def props(entityId: Int, UUID: UUID, x: Int, y: Int, z: Int, isBaby: Boolean, world: ActorRef): Props =
    Props(new ChickenImpl(entityId, UUID, x, y, z, isBaby, world))

  override def name(UUID: UUID): String = s"Chicken-$UUID"
}