package io.scalacraft.core

import java.util.UUID

import io.scalacraft.core.DataTypes.{Position, VarInt}

object TestStructures {

  import io.scalacraft.core.PacketAnnotations._

  @packet(0x0)
  case class EmptyField() extends Structure

  @packet(0x1)
  case class AllDataTypes1(bool: Boolean, @byte byte: Int, @short short: Int, int: Int, long: Long) extends Structure
  @packet(0x2)
  case class AllDataTypes2(@boxed varInt: Int, @boxed varLong: Long, float: Float, double: Double)  extends Structure
  @packet(0x3)
  case class AllDataTypes3(@maxLength(12) string: String, uuid: UUID, position: Position)  extends Structure
  @packet(0x4)
  case class AllDataTypes4(optional: Option[Int], @precededBy[Int] list: List[Int])  extends Structure

  sealed trait SwitchInterface
  @switchKey(1) case class SwitchOption1(@byte value: Int) extends SwitchInterface
  @switchKey(2) case class SwitchOption2(@short value: Int) extends SwitchInterface

  @packet(0x5)
  case class BasicSwitch(someValue: Int, @switchType[Int] switch: SwitchInterface) extends Structure

  @packet(0x6)
  case class BasicSwitchWithContext(key: Int, @switchType[Int] @fromContext(0) switch: SwitchInterface) extends Structure

  @packet(0x7)
  case class SwitchList(someValue: Int, @switchType[Int] @precededBy[Int] switch: List[SwitchInterface]) extends Structure

  @packet(0x8)
  case class SwitchOption(someValue: Int, @switchType[Int] @precededBy[Int] switch: Option[SwitchInterface]) extends Structure

  @packet(0x9)
  case class RichSwitchList(someValue: Int, @switchType[VarInt] @precededBy[VarInt] switch: List[SwitchInterface]) extends Structure





}
