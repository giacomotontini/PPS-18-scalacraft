package io.scalacraft.core

import java.util.UUID

import io.scalacraft.core.DataTypes.Position
import io.scalacraft.core.Marshallers._
import org.scalatest._

class StructureMarshallingSpec extends FlatSpec with Matchers with StructureMarshallerHelper[TestStructures.type] {
  import TestStructures._

  override def packetManager = new PacketManager[TestStructures.type]

  "A structure marshaller" should "serialize correct values" in {
    structureMarshal(EmptyField()) shouldBe ""
  }

  "A structure with " should "serialize correct values" in {
    structureMarshal(AllDataTypes1(bool = true, 1, 1, 1, 1)) shouldBe "01010001000000010000000000000001"
    structureMarshal(AllDataTypes2(1, 1, 1, 1)) shouldBe "01013f8000003ff0000000000000"
    structureMarshal(AllDataTypes3("ABC", UUID.fromString("4ff36fa0-dddb-43b1-abf7-8261824e37e2"), Position(1, 1, 1)))
      .shouldBe("034142434ff36fa0dddb43b1abf78261824e37e20000004004000001")
//    structureMarshal(AllDataTypes4(Some(1), List(1, 2, 3), Some(List(1, 2, 3)), List(Option(1), None, Option(3))))
//      .shouldBe("010000000103000000011")

  }



}




