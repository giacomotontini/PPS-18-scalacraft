package io.scalacraft.core

import java.util.UUID

import io.scalacraft.core.PacketAnnotations.{boxed, byte, particle}
import io.scalacraft.core.nbt.Tags.TagCompound

object DataTypes {
  type Chat = String
  type Identifier = String
  type Angle = Byte
  type Slot = Option[SlotData]

  case class VarInt(value: Int) extends AnyVal
  case class Position(x: Int, y: Int, z:Int)
  case class SlotData(@boxed itemId: Int, @byte itemCount: Int, nbt: Nbt) extends Structure
  case class Rotation(x: Float, y: Float, z: Float) extends Structure
  case class Nbt(name: String, compound: TagCompound)

  trait Direction
  trait Particle

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
                               field11: Direction,
                               field12: Option[UUID],
                               @boxed field13: Option[Int],
                               field14: Nbt,
                               field15: Particle) extends Structure
}
