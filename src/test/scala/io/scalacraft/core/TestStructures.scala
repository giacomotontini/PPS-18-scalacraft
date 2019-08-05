package io.scalacraft.core

import java.util.UUID

import io.scalacraft.core.DataTypes.{Position, VarInt}
import io.scalacraft.core.Entities.MobEntity

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

  sealed trait EnumInterface
  object EnumInterface {
    @enumValue(1) case object EnumOption1 extends EnumInterface
    @enumValue(2) case object EnumOption2 extends EnumInterface
  }

 
  @packet(0x10)
  case class IntTypeEnum(@enumType[Int] enumOption: EnumInterface) extends Structure
  @packet(0x11)
  case class VarIntTypeEnum(@enumType[VarInt] enumOption: EnumInterface) extends Structure
  @packet(0x12)
  case class VarIntTypeFromContextEnum(@boxed enumOption: Int, someValue: Int, @enumType[VarInt] @fromContext(0) option: EnumInterface) extends Structure

  @packet(0x13)
  case class BaseOptional(@boxed value: Option[Int]) extends Structure
  @packet(0x14)
  case class OptionalEnum( @enumType[VarInt] value: Option[EnumInterface]) extends Structure
  @packet(id= 0x15)
  case class OptionalList(@boxed someValue: Int, successful: Boolean, @byte data: List[Int]) extends Structure

  @packet(id=0x30)
  case class StructureWithMetadata(@boxed val tpe: Int, @fromContext(0) metadata: MobEntity) extends Structure

}
