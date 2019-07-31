package io.scalacraft.core

import io.scalacraft.core.PacketAnnotations.{byte, boxed}

object DataTypes {
  type Chat = String
  type Identifier = String
  case class VarInt(value: Int) extends AnyVal
  case class Position(x: Int, y: Int, z:Int)
  case class SlotData(@boxed itemId: Int, @byte itemCount: Int, nbt: Int) extends Structure
  type Slot = Option[SlotData]
}
