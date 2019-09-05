package io.scalacraft.core.marshalling

import java.io.{DataInputStream, DataOutputStream}

/**
 * The base trait of a marshaller, which define the main methods for the (un)marshalling operations.
 */
trait Marshaller {

  /**
   * If defined tell the marshaller to take the value to unmarshal from context instead of to take from the stream.
   * The number indicate the index of the field that must be read from the context.
   *
   * @return an optional index of the field in the context
   */
  protected[this] def contextFieldIndex: Option[Int]

  /**
   * Serialize the value passed as argument and write it to the stream.
   *
   * @param obj the value to serialize
   * @param outStream the stream in which the value need to be written
   */
  def marshal(obj: Any)(implicit outStream: DataOutputStream): Unit

  /**
   * Read from the stream and deserialize the value specified. If
   * [[io.scalacraft.core.marshalling.Marshaller#contextFieldIndex contextFieldIndex]] is defined, the value instead
   * of being read from the stream is read from the context.
   *
   * @param context the [[io.scalacraft.core.marshalling.Context Context]] from which the value can be read
   * @param inStream the stream from which the value can be read
   * @return the deserialized value
   */
  def unmarshal()(implicit context: Context, inStream: DataInputStream): Any = contextFieldIndex match {
    case Some(index) => context.field(index)
    case _ => internalUnmarshal()
  }

  /**
   * All marshallers must override this method to define the unmashal operation. This method is invoked when
   * [[io.scalacraft.core.marshalling.Marshaller#unmarshal unmarshal]] is called if the value to deserialize need to be
   * read from stream and not taken from context.
   *
   * @param context the stream from which the value can be read
   * @param inStream the stream from which the value can be read
   * @return the deserialized value
   */
  protected[this] def internalUnmarshal()(implicit context: Context, inStream: DataInputStream): Any

}
