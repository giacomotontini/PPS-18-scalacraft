package io.scalacraft.logic

import io.scalacraft.packets.DataTypes.Angle
import org.scalatest.{FlatSpec, Matchers}
import io.scalacraft.logic.traits.ai.general.Movement._
class ComputeCreaturesAngleSpec extends FlatSpec with Matchers{
  
  behavior of("A method that computes creature angle")

  it should "get the correct yaw angle" in {
    computeAndUpdateYawAndPitch(0, 0, -1)._1 shouldBe Angle(128)
    computeAndUpdateYawAndPitch(1, 0, 0)._1 shouldBe Angle(192)
    computeAndUpdateYawAndPitch(0, 0, 1)._1 shouldBe Angle(0)
    computeAndUpdateYawAndPitch(-1, 0, 0)._1 shouldBe Angle(64)
  }

  it should "get the correct pitch angle" in {
    computeAndUpdateYawAndPitch(deltaX = 0, deltaY = 0 , deltaZ = 0)._2 shouldBe Angle(0)
    computeAndUpdateYawAndPitch(deltaX = 0, deltaY = 1 , deltaZ = 0)._2  shouldBe Angle(-64)
    computeAndUpdateYawAndPitch(deltaX = 0, deltaY = -1 , deltaZ = 0)._2  shouldBe Angle(64)
  }

}
