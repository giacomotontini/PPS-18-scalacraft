package io.scalacraft.logic.traits.entities

import java.util.UUID

import akka.actor.ActorRef
import io.scalacraft.core.marshalling.MobsAndObjectsTypeMapping
import io.scalacraft.packets.Entities.Chicken

import scala.concurrent.duration._
import scala.util.Random

trait ChickenEntity {
  val MovementTickPeriod: FiniteDuration = 10 seconds
  val MovementFluidityFactor = 4
  val SecondInMillisecond = 1000
  private val Udm:Double = 0.0000025 // 1/8000/50
  var world: ActorRef = _
  var entityId: Int = _
  var uuid: UUID = _
  val metaData = new Chicken()
  val tpe: Int = MobsAndObjectsTypeMapping.fromMobEntityClassToType(metaData.getClass)
  var posX = 0
  var posY = 0
  var posZ = 0
  var oldPosX = 0
  var oldPosY = 0
  var oldPosZ = 0
  val pathMovesNumber = 8
  val speed = 800 //speed u.d.m is 1/8000 block per 50ms: chicken's speed is 2 block/s
  def speedInBlocksPerSecond: Double = speed * Udm * SecondInMillisecond
  def millisPerBlock: Double = SecondInMillisecond / speedInBlocksPerSecond
  def millisPerChunkOfBlock: Double = millisPerBlock / MovementFluidityFactor
  val randomGenerator: Random.type = scala.util.Random
}
