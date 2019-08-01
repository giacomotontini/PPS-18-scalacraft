package io.scalacraft.core

class Context {

  private var fields: List[Any] = List()
  def addField(obj: Any): Unit = fields :+= obj
  def field(index: Int): Any = fields(index)

}

object Context {

  def create: Context = new Context

}
