package io.scalacraft.logic.traits.creatures

import java.util.UUID

import io.scalacraft.core.marshalling.{EntityMetadata, MobsAndObjectsTypeMapping}
import io.scalacraft.packets.Entities.MobEntity

import scala.util.Random

trait CreatureParameters extends CommonParameters {
  protected val MovementFluidityFactor = 4
  protected def entityId: Int
  protected def uuid: UUID
  protected def metaData: MobEntity
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
  protected val randomGenerator: Random.type = scala.util.Random
}
