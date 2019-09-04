package io.scalacraft.loaders

import io.scalacraft.core.marshalling.PacketManager
import io.scalacraft.packets.{clientbound, serverbound}

object Packets {

  import ConnectionState._

  lazy val clientboundPackageManagers: Map[ConnectionState, PacketManager[_]] = Map(
    Status -> new PacketManager[clientbound.StatusPacket.type],
    Login -> new PacketManager[clientbound.LoginPackets.type],
    Play -> new PacketManager[clientbound.PlayPackets.type]
  )

  lazy val serverboundPackageManagers: Map[ConnectionState, PacketManager[_]] = Map(
    Handshaking -> new PacketManager[serverbound.HandshakingPackets.type],
    Status -> new PacketManager[serverbound.StatusPackets.type],
    Login -> new PacketManager[serverbound.LoginPackets.type],
    Play -> new PacketManager[serverbound.PlayPackets.type]
  )

  sealed trait ConnectionState

  object ConnectionState {

    case object Handshaking extends ConnectionState

    case object Status extends ConnectionState

    case object Login extends ConnectionState

    case object Play extends ConnectionState

  }

}
