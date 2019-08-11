package io.scalacraft.packets

import java.util.UUID

import io.scalacraft.core.marshalling.Structure
import io.scalacraft.core.marshalling.annotations.PacketAnnotations._
import net.querz.nbt.Tag

object DataTypes {
  type Chat = String
  type Identifier = String
  type Slot = Option[SlotData]
  type Nbt = Tag[_]

  case class VarInt(value: Int, length: Int)
  case class Position(x: Int, y: Int, z: Int)
  case class SlotData(@boxed itemId: Int, @byte itemCount: Int, nbt: Nbt)
  case class Rotation(x: Float, y: Float, z: Float)
  case class Angle(value: Int) extends AnyVal

  sealed trait Direction
  object Direction {
    @enumValue(0) case object Down extends Direction
    @enumValue(1) case object Up extends Direction
    @enumValue(2) case object North extends Direction
    @enumValue(3) case object South extends Direction
    @enumValue(4) case object West extends Direction
    @enumValue(5) case object East extends Direction
  }

  //Map each index of entity metadata to correspondent type
  case class entityMetadataTypes(
                                  @byte field0: Int,
                                  @boxed field1: Int,
                                  field2: Float,
                                  field3: String,
                                  field4: Chat,
                                  field5: Option[Chat],
                                  field6: Slot,
                                  field7: Boolean,
                                  field8: Rotation,
                                  field9: Position,
                                  field10: Option[Position],
                                  @enumType[VarInt] field11: Direction,
                                  field12: Option[UUID],
                                  @boxed field13: Option[Int],
                                  field14: Nbt,
                                  @switchType[VarInt] field15: ParticleData) extends Structure

  sealed trait ParticleData extends Structure
  @switchKey(3) case class Block(@boxed blockState: Int) extends ParticleData
  @switchKey(12) case class Effect() extends ParticleData

}