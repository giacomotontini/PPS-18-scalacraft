package io.scalacraft.core

import io.scalacraft.core.PacketAnnotations.{byte, boxed}
import io.scalacraft.core.nbt.Tags.TagCompound

object DataTypes {
  type Chat = String
  type Identifier = String
  case class VarInt(value: Int) extends AnyVal
  case class Position(x: Int, y: Int, z:Int)
  case class SlotData(@boxed itemId: Int, @byte itemCount: Int, nbt: Nbt) extends Structure
  type Slot = Option[SlotData]
  case class Nbt(name: String, compound: TagCompound)
}
