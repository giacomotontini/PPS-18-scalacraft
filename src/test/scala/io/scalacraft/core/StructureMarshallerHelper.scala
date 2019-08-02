package io.scalacraft.core

import java.io.{BufferedInputStream, BufferedOutputStream, ByteArrayInputStream, ByteArrayOutputStream}
import scala.reflect.runtime.universe._

trait StructureMarshallerHelper[T] {

  def packetManager: PacketManager[T]

  def structureMarshal(struct: Structure): String = {
    val serializedPacket = new ByteArrayOutputStream()
    implicit val outStream: BufferedOutputStream = new BufferedOutputStream(serializedPacket)
    packetManager.marshal(struct)
    outStream.close()

    Helpers.bytes2hex(serializedPacket.toByteArray)
  }

  def structureUnmarshal(packetId: Int, hexInput: String)(implicit context: Context = Context.create): Structure = {
    val byteArray = new ByteArrayInputStream(Helpers.hex2bytes(hexInput))
    implicit val inStream: BufferedInputStream = new BufferedInputStream(byteArray)
    val result = packetManager.unmarshal(packetId)
    inStream.close()

    result
  }

}
