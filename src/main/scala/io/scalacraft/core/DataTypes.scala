package io.scalacraft.core

object DataTypes {
  type Identifier = String
  case class VarInt(value: Int) extends AnyVal
}
