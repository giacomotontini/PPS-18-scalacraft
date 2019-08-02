package io.scalacraft.core

import io.scalacraft.core.Entities.AreaEffectCloud
import io.scalacraft.core.TestStructures.EnumInterface.{EnumOption1, EnumOption2}
import io.scalacraft.core.TestStructures.{BaseOptional, IntTypeEnum, OptionalEnum, OptionalList, StructureWithMetadata, VarIntTypeEnum, VarIntTypeFromContextEnum}
import org.scalatest.{FlatSpec, Matchers}

class StructureUnmarshallingSpec extends FlatSpec with Matchers with StructureMarshallerHelper[TestStructures.type]{
  override def packetManager = new PacketManager[TestStructures.type]

  "An int enum field" should "deserialize the correct values" in {
    structureUnmarshal(0x10, "00000001") shouldBe(IntTypeEnum(EnumOption1))
  }
  "A varint enum field" should "deserialize the correct values" in {
    structureUnmarshal(0x11, "02") shouldBe(VarIntTypeEnum(EnumOption2))
  }

  "A varint enum field from context" should "deserialize the correct values" in {
    structureUnmarshal(0x12, "0200000001") shouldBe(VarIntTypeFromContextEnum(2,1,EnumOption2))
  }

  "An optional var int field" should "deserialize the correct values" in {
    structureUnmarshal(0x13, "0102").shouldBe(BaseOptional(Some(2)))
  }

  "An optional enum of varint field" should "deserialize the correct values" in {
    structureUnmarshal(0x14, "0102").shouldBe(OptionalEnum(Some(EnumOption2)))
  }

  "A structured data with metadata field" should "deserialize the correct value" in {
    val metadata = new AreaEffectCloud()
    val packet = StructureWithMetadata(0, metadata)

    structureUnmarshal(0x30, "000000000101ac0202050003070004070005070006023f000000070100080700090f0cff").shouldBe(packet)
  }
}
