package io.scalacraft.core

import java.io.{DataInputStream, DataOutputStream, ByteArrayInputStream, ByteArrayOutputStream}

trait DataTypesMarshallerHelper {

  def dataTypesMarshal(marshaller: Marshaller, obj: Any): String = {
    val serializedPacket = new ByteArrayOutputStream()
    implicit val outStream: DataOutputStream = new DataOutputStream(serializedPacket)
    marshaller.marshal(obj)
    outStream.close()

    Helpers.bytes2hex(serializedPacket.toByteArray)
  }

  def dataTypesUnmarshal(marshaller: Marshaller, hexInput: String)(implicit context: Context = Context.create): Any = {
    val byteArray = new ByteArrayInputStream(Helpers.hex2bytes(hexInput))
    implicit val inStream: DataInputStream = new DataInputStream(byteArray)
    val result = marshaller.unmarshal()
    inStream.close()

    result
  }

}
