package io.scalacraft.core

import io.scalacraft.core.Marshallers._
import org.scalatest._

class MarshallersSpec extends FlatSpec with Matchers with MarshallerHelper {

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
    dataTypesMarshal(new ByteMarshaller, 0) shouldBe "00"
    dataTypesMarshal(new ByteMarshaller, 1) shouldBe "01"
    dataTypesMarshal(new ByteMarshaller, 255) shouldBe "ff"
  }

  "A short marshaller" should "serialize correct values" in {
    dataTypesMarshal(new ShortMarshaller, 0) shouldBe "0000"
    dataTypesMarshal(new ShortMarshaller, 1) shouldBe "0001"
    dataTypesMarshal(new ShortMarshaller, -1) shouldBe "ffff"
    dataTypesMarshal(new ShortMarshaller, Short.MinValue.toInt) shouldBe "8000"
    dataTypesMarshal(new ShortMarshaller, Short.MaxValue.toInt) shouldBe "7fff"
  }

  "An unsigned short marshaller" should "serialize correct values" in {
    dataTypesMarshal(new ShortMarshaller, 0) shouldBe "0000"
    dataTypesMarshal(new ShortMarshaller, 1) shouldBe "0001"
    dataTypesMarshal(new ShortMarshaller, 65535) shouldBe "ffff"
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

}
