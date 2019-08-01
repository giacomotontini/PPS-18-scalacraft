package io.scalacraft.core

import io.scalacraft.core.Marshallers._
import org.scalatest._

class MarshallersSpec extends FlatSpec with Matchers with MarshallerHelper {

  "A boolean marshaller" should "serialize correct values" in {
    dataTypesMarshal(BooleanMarshaller, false) shouldBe "00"
    dataTypesMarshal(BooleanMarshaller, true) shouldBe "01"
  }

  "An int marshaller" should "serialize correct values" in {
    dataTypesMarshal(IntMarshaller, 0) shouldBe "00000000"
    dataTypesMarshal(IntMarshaller, 1) shouldBe "00000001"
    dataTypesMarshal(IntMarshaller, -1) shouldBe "ffffffff"
    dataTypesMarshal(IntMarshaller, Int.MinValue) shouldBe "80000000"
    dataTypesMarshal(IntMarshaller, Int.MaxValue) shouldBe "7fffffff"
  }

  "A byte marshaller" should "serialize correct values" in {
    dataTypesMarshal(ByteMarshaller, 0) shouldBe "00"
    dataTypesMarshal(ByteMarshaller, 1) shouldBe "01"
    dataTypesMarshal(ByteMarshaller, -1) shouldBe "ff"
    dataTypesMarshal(ByteMarshaller, Byte.MinValue.toInt) shouldBe "80"
    dataTypesMarshal(ByteMarshaller, Byte.MaxValue.toInt) shouldBe "7f"
  }

  "An unsigned byte marshaller" should "serialize correct values" in {
    dataTypesMarshal(ByteMarshaller, 0) shouldBe "00"
    dataTypesMarshal(ByteMarshaller, 1) shouldBe "01"
    dataTypesMarshal(ByteMarshaller, 255) shouldBe "ff"
  }

  "A short marshaller" should "serialize correct values" in {
    dataTypesMarshal(ShortMarshaller, 0) shouldBe "0000"
    dataTypesMarshal(ShortMarshaller, 1) shouldBe "0001"
    dataTypesMarshal(ShortMarshaller, -1) shouldBe "ffff"
    dataTypesMarshal(ShortMarshaller, Short.MinValue.toInt) shouldBe "8000"
    dataTypesMarshal(ShortMarshaller, Short.MaxValue.toInt) shouldBe "7fff"
  }

  "An unsigned short marshaller" should "serialize correct values" in {
    dataTypesMarshal(ShortMarshaller, 0) shouldBe "0000"
    dataTypesMarshal(ShortMarshaller, 1) shouldBe "0001"
    dataTypesMarshal(ShortMarshaller, 65535) shouldBe "ffff"
  }

  "A long marshaller" should "serialize correct values" in {
    dataTypesMarshal(LongMarshaller, 0L) shouldBe "0000000000000000"
    dataTypesMarshal(LongMarshaller, 1L) shouldBe "0000000000000001"
    dataTypesMarshal(LongMarshaller, -1L) shouldBe "ffffffffffffffff"
    dataTypesMarshal(LongMarshaller, Long.MinValue) shouldBe "8000000000000000"
    dataTypesMarshal(LongMarshaller, Long.MaxValue) shouldBe "7fffffffffffffff"
  }

  "A float marshaller" should "serialize correct values" in {
    dataTypesMarshal(FloatMarshaller, 0f) shouldBe "00000000"
    dataTypesMarshal(FloatMarshaller, 1f) shouldBe "3f800000"
    dataTypesMarshal(FloatMarshaller, -1f) shouldBe "bf800000"
    dataTypesMarshal(FloatMarshaller, Float.MinValue) shouldBe "ff7fffff"
    dataTypesMarshal(FloatMarshaller, Float.MaxValue) shouldBe "7f7fffff"
  }

  "A double marshaller" should "serialize correct values" in {
    dataTypesMarshal(DoubleMarshaller, 0d) shouldBe "0000000000000000"
    dataTypesMarshal(DoubleMarshaller, 1d) shouldBe "3ff0000000000000"
    dataTypesMarshal(DoubleMarshaller, -1d) shouldBe "bff0000000000000"
    dataTypesMarshal(DoubleMarshaller, Double.MinValue) shouldBe "ffefffffffffffff"
    dataTypesMarshal(DoubleMarshaller, Double.MaxValue) shouldBe "7fefffffffffffff"
  }

  "A string marshaller" should "serialize correct values" in {
    dataTypesMarshal(new StringMarshaller(0), "") shouldBe "00"
    dataTypesMarshal(new StringMarshaller(4), "ABCD") shouldBe "0441424344"
    a [IllegalArgumentException] shouldBe thrownBy {
      dataTypesMarshal(new StringMarshaller(4), "ABCDEF")
    }
  }

  "A variable int marshaller" should "serialize correct values" in {
    dataTypesMarshal(VarIntMarshaller, 0) shouldBe "00"
    dataTypesMarshal(VarIntMarshaller, 1) shouldBe "01"
    dataTypesMarshal(VarIntMarshaller, -1) shouldBe "ffffffff0f"
    dataTypesMarshal(VarIntMarshaller, Int.MinValue) shouldBe "8080808008"
    dataTypesMarshal(VarIntMarshaller, Int.MaxValue) shouldBe "ffffffff07"
  }

  "A variable long marshaller" should "serialize correct values" in {
    dataTypesMarshal(VarLongMarshaller, 0L) shouldBe "00"
    dataTypesMarshal(VarLongMarshaller, 1L) shouldBe "01"
    dataTypesMarshal(VarLongMarshaller, -1L) shouldBe "ffffffffffffffffff01"
    dataTypesMarshal(VarLongMarshaller, Long.MinValue) shouldBe "80808080808080808001"
    dataTypesMarshal(VarLongMarshaller, Long.MaxValue) shouldBe "ffffffffffffffff7f"
  }

}
