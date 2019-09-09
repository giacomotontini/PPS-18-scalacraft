package io.scalacraft.core.network

import java.io.{ByteArrayInputStream, DataInputStream}
import java.util

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder

/**
 * A class responsible of message decoding.
 */
private[network] class MessageDecoder extends ByteToMessageDecoder {

  var packetLength, packetId = -1
  var numRead, result = 0

  /**
   * It solely purpose is to read messages from raw bytes accordingly to packet format.
   * Each message is made up of:
   * [Packet length: VarInt (packet id length + packet payload length)]
   * [Packet Id: VarInt]
   * [Packet payload: ByteArray]
   *
   * @param ctx not used
   * @param in  input buffer to read from
   * @param out output list on which raw packet must be added
   */
  override def decode(ctx: ChannelHandlerContext, in: ByteBuf, out: util.List[AnyRef]): Unit = {

    def readVarInt(): Boolean = {
      var terminated = false
      do {
        val read = in.readByte()
        result |= ((read & 0x7f) << (7 * numRead))
        numRead += 1
        if (numRead > 5) {
          throw new IllegalArgumentException("VarInt is too big")
        }
        terminated = (read & 0x80) == 0
      } while (!terminated && in.readableBytes() > 0)
      terminated
    }

    if (packetLength == -1 && readVarInt()) {
      packetLength = result
      result = 0
      numRead = 0
    }

    if (packetId == -1 && packetLength != -1 && readVarInt()) {
      packetId = result
      result = 0
    }

    if (in.readableBytes() >= packetLength - numRead && packetId != -1 && packetLength != -1) {
      val buff: Array[Byte] = new Array[Byte](packetLength - numRead)
      in.readBytes(buff)
      out.add(RawPacket(packetId, new DataInputStream(new ByteArrayInputStream(buff))))
      packetLength = -1
      packetId = -1
      numRead = 0
    }
  }

}
