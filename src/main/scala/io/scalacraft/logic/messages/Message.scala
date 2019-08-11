package io.scalacraft.logic.messages

import io.scalacraft.core.fsm.ConnectionState.PlayState

trait Message

object Message {

  /* --------------------------------------------- Region and chunks --------------------------------------------- */
  case class  RequestChunkData(chunkX: Int, chunkZ: Int, fullChunk: Boolean = true) extends Message
  case object ChunkNotPresent extends Message

  case class  PlayerLogged(playState: PlayState) extends Message


}
