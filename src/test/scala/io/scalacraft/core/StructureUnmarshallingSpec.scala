package io.scalacraft.core

import io.scalacraft.core.Entities.AreaEffectCloud
import io.scalacraft.core.TestStructures.EnumInterface.{EnumOption1, EnumOption2}
import io.scalacraft.core.TestStructures._
import org.scalatest.{FlatSpec, Matchers}

class StructureUnmarshallingSpec extends FlatSpec with Matchers with StructureMarshallerHelper[TestStructures.type] {

  override def packetManager = new PacketManager[TestStructures.type]

  "An int enum field" should "deserialize the correct values" in {
    structureUnmarshal(0x10, "00000001") shouldBe IntTypeEnum(EnumOption1)
  }

  "A varint enum field" should "deserialize the correct values" in {
    structureUnmarshal(0x11, "02") shouldBe VarIntTypeEnum(EnumOption2)
  }

  "A varint enum field from context" should "deserialize the correct values" in {
    structureUnmarshal(0x12, "0200000001") shouldBe VarIntTypeFromContextEnum(2, 1, EnumOption2)
  }

  "An optional var int field" should "deserialize the correct values" in {
    structureUnmarshal(0x13, "0102") shouldBe BaseOptional(Some(2))
  }

  "An optional enum of varint field" should "deserialize the correct values" in {
    structureUnmarshal(0x14, "0102") shouldBe OptionalEnum(Some(EnumOption2))
  }

  "An optional list of byte field" should "deserialize the correct values" in {
    structureUnmarshal(0x15, "01010102") shouldBe OptionalList(1, true, List(1,2))
    structureUnmarshal(0x15, "0100") shouldBe OptionalList(1,false,List())
  }

  "A basic switch" should "serialize the correct values" in {
    structureUnmarshal(0x05, "000000020000000142") shouldBe BasicSwitch(2, SwitchOption1(0x42))
  }

  "A basic switch which take key from context" should "serialize the correct values" in {
    structureUnmarshal(0x06, "000000020042") shouldBe BasicSwitchWithContext(2, SwitchOption2(0x42))
  }

  "A switch of list" should "serialize the correct values" in {
    structureUnmarshal(0x07, "000000010000000100000000") shouldBe SwitchList(1, List())
    structureUnmarshal(0x07, "00000001000000020000000200420043")
      .shouldBe(SwitchList(1, List(SwitchOption2(0x42), SwitchOption2(0x43))))
  }

  "A switch of option" should "serialize the correct values" in {
    structureUnmarshal(0x08, "000000010000000100") shouldBe SwitchOption(1, None)
    structureUnmarshal(0x08, "0000000100000002010042")
      .shouldBe(SwitchOption(1, Some(SwitchOption2(0x42))))
  }

  "A switch of list with custom key" should "serialize the correct values" in {
    structureUnmarshal(0x09, "00000001020200420043")
      .shouldBe(RichSwitchList(1, List(SwitchOption2(0x42), SwitchOption2(0x43))))
  }

  "A structured data with metadata field" should "deserialize the correct value" in {
    val metadata = new AreaEffectCloud()
    val packet = StructureWithMetadata(0, metadata)
    structureUnmarshal(0x30, "000000000101ac0202050003070004070005070006023f000000070100080700090f0cff").shouldBe(packet)
  }

  "A structured data with custom metadata field" should "deserialize the correct value" in {
    val metadata = new AreaEffectCloud()
    metadata.radius = 1
    metadata.ignoreRadiusAndShowEffectAtSinglePoint = true
    val packet = StructureWithMetadata(0, metadata)
    structureUnmarshal(0x30, "000000000101ac0202050003070004070005070006023f800000070100080701090f0cff").shouldBe(packet)
  }

}
