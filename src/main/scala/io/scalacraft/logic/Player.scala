package io.scalacraft.logic


import akka.actor.{Actor, ActorRef, Props, Timers}
import io.scalacraft.core.fsm.ConnectionState.PlayState
import io.scalacraft.core.marshalling.Structure
import io.scalacraft.misc.ServerConfiguration
import io.scalacraft.packets.DataTypes.Position
import io.scalacraft.packets.clientbound.PlayPackets.{JoinGame, PlayerPositionAndLook, SpawnPosition, WorldDimension}
import io.scalacraft.packets.serverbound.PlayPackets.{ClientSettings, ClientStatus, ClientStatusAction, KeepAlive, MainHand, TeleportConfirm}

import scala.concurrent.duration._
import scala.util.Random

class Player(playState: PlayState) extends Actor with Timers {
  import Player._
  import context._
  var userContext: ActorRef = _

  private val entityId = 0
  private val worldDimension = WorldDimension.Overworld
  private var posX: Int = 0
  private var posY: Int = 0
  private var posZ: Int = 0
  private var yaw: Float = 0.0f
  private var pitch: Float = 0.0f
  //private var digging: Boolean = false
  private var onGround = true
  private var mainHand: MainHand = _
  private var locale: String = _
  private var viewDistance: Int = _ //render distance in chunks

  private var lastKeepAliveId: Long = _
  private var teleportId: Int = _
  private var playerPositionAndLookToBeConfirmed: PlayerPositionAndLook = _

  private val randomGenerator = Random

  override def preStart(): Unit = {
    userContext = context.actorOf(UserContext.props(playState))

    userContext ! JoinGame(entityId, ServerConfiguration.GameMode, worldDimension, ServerConfiguration.ServerDifficulty, ServerConfiguration.MaxPlayers, ServerConfiguration.LevelTypeBiome, ServerConfiguration.ReducedDebugInfo)
  }

  private def preStartBehaviour: Receive = {
    case clientSettings: ClientSettings =>
      locale = clientSettings.locale
      mainHand = clientSettings.mainHand
      viewDistance = clientSettings.viewDistance

      // Send chunk data from chunk actor

      userContext ! SpawnPosition(Position(posX, posY, posZ))

      teleportId = randomGenerator.nextInt()
      val flags = playerPositionAndLookFlags(xRelative = false, yRelative = false, zRelative = false, Y_ROT = false, X_ROT = false)
      playerPositionAndLookToBeConfirmed =  PlayerPositionAndLook(posX, posY, posZ, yaw, pitch, flags, teleportId)
      userContext ! playerPositionAndLookToBeConfirmed

      timers.startPeriodicTimer(KeepAliveTickKey, KeepAliveTick, 5.seconds)
      become(playingBehaviour)
    case teleportConfirm: TeleportConfirm if  teleportConfirm.teleportId == teleportId => userContext ! playerPositionAndLookToBeConfirmed
    case clientStatus : ClientStatus if clientStatus.action == ClientStatusAction.PerformRespawn => println("Respawn not to be handled on login")
    case clientStatus : ClientStatus if clientStatus.action == ClientStatusAction.RequestStats => println("User request statistics, not handled")
  }

  private def playingBehaviour: Receive = {
    case KeepAliveTick =>
      lastKeepAliveId = randomGenerator.nextLong()
      userContext ! io.scalacraft.packets.clientbound.PlayPackets.KeepAlive(lastKeepAliveId)
      if(!timers.isTimerActive(KeepAliveTimeoutKey)) {
        timers.startSingleTimer(KeepAliveTimeoutKey, KeepAliveTimeout, 30.seconds)
      }
    case KeepAliveTimeout =>
      timers.cancel(KeepAliveTickKey)
      println("Client timeout")
    case keepAlive: KeepAlive if keepAlive.keepAliveId == lastKeepAliveId =>
      timers.cancel(KeepAliveTimeoutKey)
      println("KeepAlive received, everything ok")
    case keepAlive: KeepAlive if keepAlive.keepAliveId != lastKeepAliveId => println("Received a keep alive with different value, that's bad") //tobe removed

    case player: io.scalacraft.packets.serverbound.PlayPackets.Player =>
      onGround = player.onGround
    case playerPosition: io.scalacraft.packets.serverbound.PlayPackets.PlayerPosition =>
      posX = playerPosition.x.toInt
      posY = playerPosition.feetY.toInt
      posZ = playerPosition.z.toInt
      onGround = playerPosition.onGround
    case playerPositionAndLook: io.scalacraft.packets.serverbound.PlayPackets.PlayerPositionAndLook =>
      posX = playerPositionAndLook.x.toInt
      posY = playerPositionAndLook.feetY.toInt
      posZ = playerPositionAndLook.z.toInt
      yaw = playerPositionAndLook.yaw
      pitch =playerPositionAndLook.pitch
      onGround = playerPositionAndLook.onGround
  }

  override def receive: Receive = preStartBehaviour

  private def playerPositionAndLookFlags(xRelative: Boolean, yRelative: Boolean, zRelative: Boolean, Y_ROT: Boolean, X_ROT: Boolean): Byte = {
    var flags: Byte = 0x00
    if (xRelative) flags = (flags | 0x01).toByte
    if (yRelative) flags = (flags | 0x02).toByte
    if (zRelative) flags = (flags |  0x04).toByte
    if (Y_ROT) flags = (flags |  0x08).toByte
    if (X_ROT) flags = (flags |  0x10).toByte
    flags
  }

}

object Player {
  private case object KeepAliveTickKey
  private case object KeepAliveTick
  private case object KeepAliveTimeout
  private case object KeepAliveTimeoutKey

  def props(playState: PlayState): Props = Props(new Player(playState))
}
