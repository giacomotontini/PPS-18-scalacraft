package io.scalacraft.core.network

import java.io.{ByteArrayOutputStream, DataOutputStream}

import io.netty.buffer.ByteBufOutputStream
import io.netty.channel.ChannelHandlerContext
import io.scalacraft.misc.Helpers
import io.scalacraft.packets.DataTypes.VarInt

trait ConnectionManager {

  def writePacket(dataToPacketId: DataOutputStream => VarInt): Unit

  def closeConnection(): Unit

}

object ConnectionManager {

  def apply(chc: ChannelHandlerContext): ConnectionManager = new ConnectionManagerImpl(chc)

  class ConnectionManagerImpl(channelHandlerContext: ChannelHandlerContext) extends ConnectionManager {

    override def writePacket(dataBuffer: DataOutputStream => VarInt): Unit = {
      //this buffer will be sent over the channel
      val channelByteBuf = channelHandlerContext.alloc().buffer()
      val channelByteBufOutputStream = new ByteBufOutputStream(channelByteBuf)

      //this buffer will contains packet payload data
      val buffer = new ByteArrayOutputStream

      //dataBuffer is a function that writes payload byte and buffer and return the packetId
      val outStream = new DataOutputStream(buffer)
      val packetId = dataBuffer(outStream)
      outStream.close()

      //write on channelByteBuf packetLength, packetId, payload respectively
      Helpers.writeVarInt(packetId.length + buffer.size(), channelByteBufOutputStream)
      Helpers.writeVarInt(packetId.value, channelByteBufOutputStream)
      channelByteBufOutputStream.close()
      channelByteBuf.writeBytes(buffer.toByteArray)

      //write and send channelByteBuf over the channel
      channelHandlerContext.writeAndFlush(channelByteBuf)
    }

    override def closeConnection(): Unit = channelHandlerContext.close()

  }

}
