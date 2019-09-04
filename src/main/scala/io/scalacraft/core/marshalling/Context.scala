package io.scalacraft.core.marshalling

private[marshalling] trait Context {

  def addField(obj: Any): Unit

  def field(index: Int): Any

}

private[marshalling] object Context {

  private val trashContext = new Context {
    override def addField(obj: Any): Unit = {}

    override def field(index: Int): Any = {}
  }

  def create: Context = new Context {
    private var fields: List[Any] = List()

    override def addField(obj: Any): Unit = fields :+= obj

    override def field(index: Int): Any = fields(index)
  }

  def trash: Context = trashContext

}
