package io.scalacraft.core

import java.util.UUID

import io.scalacraft.core.DataTypes.Position

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
//  @packet(0x4)
//  case class AllDataTypes4(optional: Option[Int], @precededBy[Int] list: List[Int], optionalList: Option[List[Int]], listOfOption: List[Option[Int]])  extends Structure


}
