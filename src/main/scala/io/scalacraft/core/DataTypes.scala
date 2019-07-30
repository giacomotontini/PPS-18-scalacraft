package io.scalacraft.core

object DataTypes {
  type Chat = String
  type Identifier = String
  case class VarInt(value: Int) extends AnyVal
}
