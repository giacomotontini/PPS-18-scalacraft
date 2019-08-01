package io.scalacraft.core

import java.io.{BufferedOutputStream, ByteArrayOutputStream}

trait MarshallerHelper {

  def dataTypesMarshal(marshaller: Marshaller, obj: Any): String = {
    val serializedPacket = new ByteArrayOutputStream()
    implicit val outStream: BufferedOutputStream = new BufferedOutputStream(serializedPacket)
    marshaller.marshal(obj)
    outStream.close()

    Helpers.bytes2hex(serializedPacket.toByteArray)
  }

}
