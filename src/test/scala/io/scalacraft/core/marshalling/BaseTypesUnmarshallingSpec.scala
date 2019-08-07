package io.scalacraft.core.marshalling

import java.util.UUID

import io.scalacraft.core.marshalling.Marshallers._
import io.scalacraft.packets.DataTypes.Position
import org.scalatest._

class BaseTypesUnmarshallingSpec extends FlatSpec with Matchers with DataTypesMarshallerHelper {

  "A boolean marshaller" should "deserialize correct values" in {
    dataTypesUnmarshal(new BooleanMarshaller, "00") shouldBe false
    dataTypesUnmarshal(new BooleanMarshaller, "01") shouldBe true
  }

  "An int marshaller" should "deserialize correct values" in {
    dataTypesUnmarshal(new IntMarshaller, "00000000") shouldBe 0
    dataTypesUnmarshal(new IntMarshaller, "00000001") shouldBe 1
    dataTypesUnmarshal(new IntMarshaller, "ffffffff") shouldBe -1
    dataTypesUnmarshal(new IntMarshaller, "80000000") shouldBe Int.MinValue
    dataTypesUnmarshal(new IntMarshaller, "7fffffff") shouldBe Int.MaxValue
  }

  "A byte marshaller" should "deserialize correct values" in {
    dataTypesUnmarshal(new ByteMarshaller, "00") shouldBe 0
    dataTypesUnmarshal(new ByteMarshaller, "01") shouldBe 1
    dataTypesUnmarshal(new ByteMarshaller, "ff") shouldBe -1
    dataTypesUnmarshal(new ByteMarshaller, "80") shouldBe Byte.MinValue.toInt
    dataTypesUnmarshal(new ByteMarshaller, "7f") shouldBe Byte.MaxValue.toInt
  }

  "An unsigned byte marshaller" should "deserialize correct values" in {
    dataTypesUnmarshal(new ByteMarshaller(true), "00") shouldBe 0
    dataTypesUnmarshal(new ByteMarshaller(true), "01") shouldBe 1
    dataTypesUnmarshal(new ByteMarshaller(true), "ff") shouldBe 255
  }

  "A short marshaller" should "deserialize correct values" in {
    dataTypesUnmarshal(new ShortMarshaller, "0000") shouldBe 0
    dataTypesUnmarshal(new ShortMarshaller, "0001") shouldBe 1
    dataTypesUnmarshal(new ShortMarshaller, "ffff") shouldBe -1
    dataTypesUnmarshal(new ShortMarshaller, "8000") shouldBe Short.MinValue
    dataTypesUnmarshal(new ShortMarshaller, "7fff") shouldBe Short.MaxValue
  }

  "An unsigned short marshaller" should "deserialize correct values" in {
    dataTypesUnmarshal(new ShortMarshaller(true), "0000") shouldBe 0
    dataTypesUnmarshal(new ShortMarshaller(true), "0001") shouldBe 1
    dataTypesUnmarshal(new ShortMarshaller(true), "ffff") shouldBe 65535
  }

  "A long marshaller" should "deserialize correct values" in {
    dataTypesUnmarshal(new LongMarshaller, "0000000000000000") shouldBe 0L
    dataTypesUnmarshal(new LongMarshaller, "0000000000000001") shouldBe 1L
    dataTypesUnmarshal(new LongMarshaller, "ffffffffffffffff") shouldBe -1L
    dataTypesUnmarshal(new LongMarshaller, "8000000000000000") shouldBe Long.MinValue
    dataTypesUnmarshal(new LongMarshaller, "7fffffffffffffff") shouldBe Long.MaxValue
  }

  "A float marshaller" should "deserialize correct values" in {
    dataTypesUnmarshal(new FloatMarshaller, "00000000") shouldBe 0f
    dataTypesUnmarshal(new FloatMarshaller, "3f800000") shouldBe 1f
    dataTypesUnmarshal(new FloatMarshaller, "bf800000") shouldBe -1f
    dataTypesUnmarshal(new FloatMarshaller, "ff7fffff") shouldBe Float.MinValue
    dataTypesUnmarshal(new FloatMarshaller, "7f7fffff") shouldBe Float.MaxValue
  }

  "A double marshaller" should "deserialize correct values" in {
    dataTypesUnmarshal(new DoubleMarshaller, "0000000000000000") shouldBe 0d
    dataTypesUnmarshal(new DoubleMarshaller, "3ff0000000000000") shouldBe 1d
    dataTypesUnmarshal(new DoubleMarshaller, "bff0000000000000") shouldBe -1d
    dataTypesUnmarshal(new DoubleMarshaller, "ffefffffffffffff") shouldBe Double.MinValue
    dataTypesUnmarshal(new DoubleMarshaller, "7fefffffffffffff") shouldBe Double.MaxValue
  }

  "A string marshaller" should "deserialize correct values" in {
    dataTypesUnmarshal(new StringMarshaller(0), "00") shouldBe ""
    dataTypesUnmarshal(new StringMarshaller(4), "0441424344") shouldBe "ABCD"
    a [IllegalArgumentException] shouldBe thrownBy {
      dataTypesUnmarshal(new StringMarshaller(4), "0641424344454647")
    }
  }

  "A variable int marshaller" should "deserialize correct values" in {
    dataTypesUnmarshal(new VarIntMarshaller, "00") shouldBe 0
    dataTypesUnmarshal(new VarIntMarshaller, "01") shouldBe 1
    dataTypesUnmarshal(new VarIntMarshaller, "ffffffff0f") shouldBe -1
    dataTypesUnmarshal(new VarIntMarshaller, "8080808008") shouldBe Int.MinValue
    dataTypesUnmarshal(new VarIntMarshaller, "ffffffff07") shouldBe Int.MaxValue
  }

  "A variable long marshaller" should "deserialize correct values" in {
    dataTypesUnmarshal(new VarLongMarshaller, "00") shouldBe 0L
    dataTypesUnmarshal(new VarLongMarshaller, "01") shouldBe 1L
    dataTypesUnmarshal(new VarLongMarshaller, "ffffffffffffffffff01") shouldBe -1L
    //dataTypesUnmarshal(new VarLongMarshaller, "80808080808080808001") shouldBe Long.MinValue
    //dataTypesUnmarshal(new VarLongMarshaller, "ffffffffffffffff7f") shouldBe Long.MaxValue
  }

  "A position marshaller" should "deserialize correct values" in {
    dataTypesUnmarshal(new PositionMarshaller, "0000000000000000") shouldBe Position(0, 0, 0)
    dataTypesUnmarshal(new PositionMarshaller, "0000004004000001") shouldBe Position(1, 1, 1)
    //dataTypesUnmarshal(new PositionMarshaller, "ffffffffffffffff") shouldBe Position(-1, -1, -1)
    //dataTypesUnmarshal(new PositionMarshaller, "8000002002000000") shouldBe Position(-33554432, -2048, -33554432)
    dataTypesUnmarshal(new PositionMarshaller, "7fffffdffdffffff") shouldBe Position(33554431, 2047, 33554431)
  }

  "An UUID marshaller" should "deserialize correct values" in {
    dataTypesUnmarshal(new UUIDMarshaller, "4ff36fa0dddb43b1abf78261824e37e2")
      .shouldBe(UUID.fromString("4ff36fa0-dddb-43b1-abf7-8261824e37e2"))
  }

  "An optional marshaller" should "deserialize correct values" in {
    dataTypesUnmarshal(new OptionalMarshaller(new IntMarshaller), "00") shouldBe None
    dataTypesUnmarshal(new OptionalMarshaller(new IntMarshaller), "0100000042") shouldBe Option(0x42)
    dataTypesUnmarshal(new OptionalMarshaller(new IntMarshaller, Some(new IntMarshaller)), "0000000100000042")
      .shouldBe(Option(0x42))
  }

  "A list marshaller" should "deserialize correct values" in {
    dataTypesUnmarshal(new ListMarshaller(new IntMarshaller, None), "") shouldBe List()
    dataTypesUnmarshal(new ListMarshaller(new IntMarshaller, Some(new IntMarshaller)), "00000000")
      .shouldBe(List())
    dataTypesUnmarshal(new ListMarshaller(new IntMarshaller, None), "000000010000000200000003")
      .shouldBe(List(1, 2, 3))
    dataTypesUnmarshal(new ListMarshaller(new IntMarshaller, Some(new IntMarshaller)), "00000003000000010000000200000003")
      .shouldBe(List(1, 2, 3))
  }

  // Non sense but exists in protocol..

  "A list of optional marshaller" should "deserialize correct values" in {
    val optionalMarshaller = new OptionalMarshaller(new IntMarshaller)
    dataTypesMarshal(new ListMarshaller(optionalMarshaller, None), List())
      .shouldBe("")
    dataTypesMarshal(new ListMarshaller(optionalMarshaller, Some(new IntMarshaller)), List())
      .shouldBe("00000000")
    dataTypesMarshal(new ListMarshaller(optionalMarshaller, None), List(Some(1), None, Some(3)))
      .shouldBe("0100000001000100000003")
    dataTypesMarshal(new ListMarshaller(optionalMarshaller, Some(new IntMarshaller)), List(Some(1), None, Some(3)))
      .shouldBe("000000030100000001000100000003")
  }

  "An optional list of marshaller" should "deserialize correct values" in {
    val arrayMarshaller = new ListMarshaller(new IntMarshaller, None)
    dataTypesMarshal(new OptionalMarshaller(arrayMarshaller), None) shouldBe "00"
    dataTypesMarshal(new OptionalMarshaller(arrayMarshaller), Some(List(1, 3))) shouldBe "010000000100000003"
    dataTypesMarshal(new OptionalMarshaller(arrayMarshaller, Some(new IntMarshaller)), Some(List(1, 3)))
      .shouldBe("0000000100000003")
  }

}
