package io.scalacraft.core

import java.util.UUID

import io.scalacraft.core.DataTypes.Position
import io.scalacraft.core.Entities.AreaEffectCloud
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

  "A switch field" should "serialize the correct values" in {
    structureMarshal(BasicSwitch(2, SwitchOption1(0x42))) shouldBe "000000020000000142"
    structureMarshal(BasicSwitchWithContext(2, SwitchOption2(0x42))) shouldBe "000000020042"
    structureMarshal(SwitchList(1, List())) shouldBe "000000010000000100000000"
    structureMarshal(SwitchList(1, List(SwitchOption2(0x42), SwitchOption2(0x43))))
      .shouldBe("00000001000000020000000200420043")
    structureMarshal(SwitchOption(1, None)).shouldBe("000000010000000100")
    structureMarshal(SwitchOption(1, Some(SwitchOption2(0x42)))).shouldBe("0000000100000002010042")
    structureMarshal(RichSwitchList(1, List(SwitchOption2(0x42), SwitchOption2(0x43))))
      .shouldBe("00000001020200420043")
  }

//  "A structure with metadata " should "serialize correct values" in {
//    val metadata = new AreaEffectCloud()
//    val packet = StructureWithMetadata(metadata)
//    structureMarshal(packet) shouldBe ""
//  }

}
