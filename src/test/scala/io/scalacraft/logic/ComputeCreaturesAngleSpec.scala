package io.scalacraft.logic

import io.scalacraft.logic.creatures.behaviours.Movement.computeYawAndPitch
import io.scalacraft.core.packets.DataTypes.Angle
import org.scalatest.{FlatSpec, Matchers}

class ComputeCreaturesAngleSpec extends FlatSpec with Matchers {

  "A method that computes creature angle" should "get the correct yaw angle" in {
    computeYawAndPitch(0, 0, -1)._1 shouldBe Angle(128)
    computeYawAndPitch(1, 0, 0)._1 shouldBe Angle(192)
    computeYawAndPitch(0, 0, 1)._1 shouldBe Angle(0)
    computeYawAndPitch(-1, 0, 0)._1 shouldBe Angle(64)
  }

  it should "get the correct pitch angle" in {
    computeYawAndPitch(deltaX = 0, deltaY = 0, deltaZ = 0)._2 shouldBe Angle(0)
    computeYawAndPitch(deltaX = 0, deltaY = 1, deltaZ = 0)._2 shouldBe Angle(-64)
    computeYawAndPitch(deltaX = 0, deltaY = -1, deltaZ = 0)._2 shouldBe Angle(64)
  }

}
