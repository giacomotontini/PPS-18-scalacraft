package io.scalacraft.core.marshalling

/**
 * Contains the values of the already deserialized fields of an object during an unmashalling operation.
 * Used to access at the previous deserialized fields in next stages of unmashalling process.
 */
private[marshalling] trait Context {

  /**
   * Add the value of a field to context.
   *
   * @param obj the value of the field
   */
  def addField(obj: Any): Unit

  /**
   * Retrieve the value of a field.
   *
   * @param index the index of the field to retrieve
   * @return the value of the field
   */
  def field(index: Int): Any

}

/**
 * Contains some factory methods to create predefine [[io.scalacraft.core.marshalling.Context Context]].
 */
private[marshalling] object Context {

  private val trashContext = new Context {
    override def addField(obj: Any): Unit = {}

    override def field(index: Int): Any = new UnsupportedOperationException
  }

  /**
   * Create an empty [[io.scalacraft.core.marshalling.Context Context]].
   *
   * @return the empty context newly created
   */
  def create: Context = new Context {
    private var fields: List[Any] = List()

    override def addField(obj: Any): Unit = fields :+= obj

    override def field(index: Int): Any = fields(index)
  }

  /**
   * Retrieve the trash [[io.scalacraft.core.marshalling.Context Context]], a special context which throw away all
   * request to store values of fields.
   *
   * @return the trash context
   */
  def trash: Context = trashContext

}
