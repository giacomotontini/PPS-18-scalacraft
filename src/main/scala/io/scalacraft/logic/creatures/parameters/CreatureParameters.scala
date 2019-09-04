package io.scalacraft.logic.creatures.parameters

import java.util.UUID

import io.scalacraft.core.marshalling.MobsAndObjectsTypeMapping
import io.scalacraft.packets.Entities.MobEntity

import scala.util.Random

trait CreatureParameters[T <: MobEntity] extends CommonParameters {

  protected val MovementFluidityFactor = 4
  protected def entityId: Int
  protected def uuid: UUID
  protected def metaData: T
  protected lazy val tpe: Int = MobsAndObjectsTypeMapping.fromMobEntityClassToType(metaData.getClass)
  protected var posX = 0
  protected var posY = 0
  protected var posZ = 0
  protected var oldPosX = 0
  protected var oldPosY = 0
  protected var oldPosZ = 0
  protected def pathMovesNumber: Int
  protected def speed: Int
  protected def speedInBlocksPerSecond: Double = speed * Udm * SecondInMillisecond
  protected def millisPerBlock: Double = SecondInMillisecond / speedInBlocksPerSecond
  protected def millisPerChunkOfBlock: Double = millisPerBlock / MovementFluidityFactor
  protected val randomGenerator: Random = scala.util.Random

}

object CreatureParameters {

  val SoundEffectPositionMultiplier = 8
  val SoundVolume = 1
  val SoundPitch = 0.5f

}