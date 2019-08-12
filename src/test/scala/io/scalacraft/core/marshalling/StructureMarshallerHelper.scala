package io.scalacraft.core.marshalling

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, DataInputStream, DataOutputStream}

import io.scalacraft.misc.Helpers

trait StructureMarshallerHelper[T] {

  def packetManager: PacketManager[T]

  def structureMarshal(struct: Structure): String = {
    val serializedPacket = new ByteArrayOutputStream()
    implicit val outStream: DataOutputStream = new DataOutputStream(serializedPacket)
    packetManager.marshal(struct)
    outStream.close()

    Helpers.bytes2hex(serializedPacket.toByteArray)
  }

  def structureUnmarshal(packetId: Int, hexInput: String)(implicit context: Context = Context.create): Structure = {
    val byteArray = new ByteArrayInputStream(Helpers.hex2bytes(hexInput))
    implicit val inStream: DataInputStream = new DataInputStream(byteArray)
    val result = packetManager.unmarshal(packetId)
    inStream.close()

    result
  }

}
