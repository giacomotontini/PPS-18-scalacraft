package io.scalacraft.logic

import io.scalacraft.packets.DataTypes.Angle
import org.scalatest.{FlatSpec, Matchers}

class ComputeCreaturesAngleSpec extends FlatSpec with Matchers{

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

  behavior of("A method that computes creature angle")

  it should "get the correct yaw angle" in {
    println("a")
    computeYawAndPitch(0, 0, -1)._1 shouldBe Angle(128)
    computeYawAndPitch(1, 0, 0)._1 shouldBe Angle(192)
    computeYawAndPitch(0, 0, 1)._1 shouldBe Angle(0)
    computeYawAndPitch(-1, 0, 0)._1 shouldBe Angle(64)
  }

  it should "get the correct pitch angle" in {
    println("aa")
    computeYawAndPitch(deltaX = 0, deltaY = 0 , deltaZ = 0)._2 shouldBe Angle(0)
    computeYawAndPitch(deltaX = 0, deltaY = 1 , deltaZ = 0)._2  shouldBe Angle(-64)
    computeYawAndPitch(deltaX = 0, deltaY = -1 , deltaZ = 0)._2  shouldBe Angle(64)
  }

}
