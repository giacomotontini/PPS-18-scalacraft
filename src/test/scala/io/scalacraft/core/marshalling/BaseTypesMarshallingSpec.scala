package io.scalacraft.core.marshalling

import java.util.UUID

import io.scalacraft.core.marshalling.Marshallers._
import io.scalacraft.packets.DataTypes.Position
import org.scalatest._

class BaseTypesMarshallingSpec extends FlatSpec with Matchers with DataTypesMarshallerHelper {

  "A boolean marshaller" should "serialize correct values" in {
    dataTypesMarshal(new BooleanMarshaller, false) shouldBe "00"
    dataTypesMarshal(new BooleanMarshaller, true) shouldBe "01"
  }

  "An int marshaller" should "serialize correct values" in {
    dataTypesMarshal(new IntMarshaller, 0) shouldBe "00000000"
    dataTypesMarshal(new IntMarshaller, 1) shouldBe "00000001"
    dataTypesMarshal(new IntMarshaller, -1) shouldBe "ffffffff"
    dataTypesMarshal(new IntMarshaller, Int.MinValue) shouldBe "80000000"
    dataTypesMarshal(new IntMarshaller, Int.MaxValue) shouldBe "7fffffff"
  }

  "A byte marshaller" should "serialize correct values" in {
    dataTypesMarshal(new ByteMarshaller, 0) shouldBe "00"
    dataTypesMarshal(new ByteMarshaller, 1) shouldBe "01"
    dataTypesMarshal(new ByteMarshaller, -1) shouldBe "ff"
    dataTypesMarshal(new ByteMarshaller, Byte.MinValue.toInt) shouldBe "80"
    dataTypesMarshal(new ByteMarshaller, Byte.MaxValue.toInt) shouldBe "7f"
  }

  "An unsigned byte marshaller" should "serialize correct values" in {
    dataTypesMarshal(new ByteMarshaller(true), 0) shouldBe "00"
    dataTypesMarshal(new ByteMarshaller(true), 1) shouldBe "01"
    dataTypesMarshal(new ByteMarshaller(true), 255) shouldBe "ff"
  }

  "A short marshaller" should "serialize correct values" in {
    dataTypesMarshal(new ShortMarshaller, 0) shouldBe "0000"
    dataTypesMarshal(new ShortMarshaller, 1) shouldBe "0001"
    dataTypesMarshal(new ShortMarshaller, -1) shouldBe "ffff"
    dataTypesMarshal(new ShortMarshaller, Short.MinValue.toInt) shouldBe "8000"
    dataTypesMarshal(new ShortMarshaller, Short.MaxValue.toInt) shouldBe "7fff"
  }

  "An unsigned short marshaller" should "serialize correct values" in {
    dataTypesMarshal(new ShortMarshaller(true), 0) shouldBe "0000"
    dataTypesMarshal(new ShortMarshaller(true), 1) shouldBe "0001"
    dataTypesMarshal(new ShortMarshaller(true), 65535) shouldBe "ffff"
  }

  "A long marshaller" should "serialize correct values" in {
    dataTypesMarshal(new LongMarshaller, 0L) shouldBe "0000000000000000"
    dataTypesMarshal(new LongMarshaller, 1L) shouldBe "0000000000000001"
    dataTypesMarshal(new LongMarshaller, -1L) shouldBe "ffffffffffffffff"
    dataTypesMarshal(new LongMarshaller, Long.MinValue) shouldBe "8000000000000000"
    dataTypesMarshal(new LongMarshaller, Long.MaxValue) shouldBe "7fffffffffffffff"
  }

  "A float marshaller" should "serialize correct values" in {
    dataTypesMarshal(new FloatMarshaller, 0f) shouldBe "00000000"
    dataTypesMarshal(new FloatMarshaller, 1f) shouldBe "3f800000"
    dataTypesMarshal(new FloatMarshaller, -1f) shouldBe "bf800000"
    dataTypesMarshal(new FloatMarshaller, Float.MinValue) shouldBe "ff7fffff"
    dataTypesMarshal(new FloatMarshaller, Float.MaxValue) shouldBe "7f7fffff"
  }

  "A double marshaller" should "serialize correct values" in {
    dataTypesMarshal(new DoubleMarshaller, 0d) shouldBe "0000000000000000"
    dataTypesMarshal(new DoubleMarshaller, 1d) shouldBe "3ff0000000000000"
    dataTypesMarshal(new DoubleMarshaller, -1d) shouldBe "bff0000000000000"
    dataTypesMarshal(new DoubleMarshaller, Double.MinValue) shouldBe "ffefffffffffffff"
    dataTypesMarshal(new DoubleMarshaller, Double.MaxValue) shouldBe "7fefffffffffffff"
  }

  "A string marshaller" should "serialize correct values" in {
    dataTypesMarshal(new StringMarshaller(0), "") shouldBe "00"
    dataTypesMarshal(new StringMarshaller(4), "ABCD") shouldBe "0441424344"
    a [IllegalArgumentException] shouldBe thrownBy {
      dataTypesMarshal(new StringMarshaller(4), "ABCDEF")
    }
  }

  "A variable int marshaller" should "serialize correct values" in {
    dataTypesMarshal(new VarIntMarshaller, 0) shouldBe "00"
    dataTypesMarshal(new VarIntMarshaller, 1) shouldBe "01"
    dataTypesMarshal(new VarIntMarshaller, -1) shouldBe "ffffffff0f"
    dataTypesMarshal(new VarIntMarshaller, Int.MinValue) shouldBe "8080808008"
    dataTypesMarshal(new VarIntMarshaller, Int.MaxValue) shouldBe "ffffffff07"
  }

  "A variable long marshaller" should "serialize correct values" in {
    dataTypesMarshal(new VarLongMarshaller, 0L) shouldBe "00"
    dataTypesMarshal(new VarLongMarshaller, 1L) shouldBe "01"
    dataTypesMarshal(new VarLongMarshaller, -1L) shouldBe "ffffffffffffffffff01"
    dataTypesMarshal(new VarLongMarshaller, Long.MinValue) shouldBe "80808080808080808001"
    dataTypesMarshal(new VarLongMarshaller, Long.MaxValue) shouldBe "ffffffffffffffff7f"
  }

  "A position marshaller" should "serialize correct values" in {
    dataTypesMarshal(new PositionMarshaller, Position(0, 0, 0)) shouldBe "0000000000000000"
    dataTypesMarshal(new PositionMarshaller, Position(1, 1, 1)) shouldBe "0000004004000001"
    dataTypesMarshal(new PositionMarshaller, Position(-1, -1, -1)) shouldBe "ffffffffffffffff"
    dataTypesMarshal(new PositionMarshaller, Position(-33554432, -2048, -33554432)) shouldBe "8000002002000000"
    dataTypesMarshal(new PositionMarshaller, Position(33554431, 2047, 33554431)) shouldBe "7fffffdffdffffff"
  }

  "An UUID marshaller" should "serialize correct values" in {
    dataTypesMarshal(new UUIDMarshaller, UUID.fromString("4ff36fa0-dddb-43b1-abf7-8261824e37e2"))
      .shouldBe("4ff36fa0dddb43b1abf78261824e37e2")
  }

  "An optional marshaller" should "serialize correct values" in {
    dataTypesMarshal(new OptionalMarshaller(new IntMarshaller), None) shouldBe "00"
    dataTypesMarshal(new OptionalMarshaller(new IntMarshaller), Option(0x42)) shouldBe "0100000042"
    // the condition is not written to output
    dataTypesMarshal(new OptionalMarshaller(new IntMarshaller, Some(new IntMarshaller)), Option(0x42)) shouldBe "00000042"
  }

  "A byte array marshaller" should "serialize correct values" in {
    dataTypesMarshal(new ByteArrayMarshaller(None), Array[Byte]()) shouldBe ""
    dataTypesMarshal(new ByteArrayMarshaller(Some(new IntMarshaller)), Array[Byte]()) shouldBe "00000000"
    dataTypesMarshal(new ByteArrayMarshaller(None), Array(1.toByte, 2.toByte, 3.toByte)) shouldBe "010203"
    dataTypesMarshal(new ByteArrayMarshaller(Some(new IntMarshaller)), Array(1.toByte, 2.toByte, 3.toByte))
      .shouldBe("00000003010203")
  }

  "A list marshaller" should "serialize correct values" in {
    dataTypesMarshal(new ListMarshaller(new IntMarshaller, None), List())
      .shouldBe("")
    dataTypesMarshal(new ListMarshaller(new IntMarshaller, Some(new IntMarshaller)), List())
      .shouldBe("00000000")
    dataTypesMarshal(new ListMarshaller(new IntMarshaller, None), List(1, 2, 3))
      .shouldBe("000000010000000200000003")
    dataTypesMarshal(new ListMarshaller(new IntMarshaller, Some(new IntMarshaller)), List(1, 2, 3))
      .shouldBe("00000003000000010000000200000003")
  }

  // Non sense but exists in protocol..

  "An array of optional marshaller" should "serialize correct values" in {
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

  "An optional of array marshaller" should "serialize correct values" in {
    val listMarshaller = new ListMarshaller(new IntMarshaller, None)
    dataTypesMarshal(new OptionalMarshaller(listMarshaller), None) shouldBe "00"
    dataTypesMarshal(new OptionalMarshaller(listMarshaller), Some(List(1, 3))) shouldBe "010000000100000003"
    dataTypesMarshal(new OptionalMarshaller(listMarshaller, Some(new IntMarshaller)), Some(List(1, 3)))
      .shouldBe("0000000100000003")
  }

}
