package io.scalacraft.logic

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
import akka.pattern._
import io.scalacraft.core.marshalling.Structure
import io.scalacraft.core.network.{ConnectionManager, RawPacket}
import io.scalacraft.loaders.Packets
import io.scalacraft.loaders.Packets.ConnectionState
import io.scalacraft.logic.messages.Message._
import io.scalacraft.logic.inventories.traits.{DefaultTimeout, ImplicitContext}
import io.scalacraft.misc.ServerConfiguration
import io.scalacraft.packets.clientbound.LoginPackets.LoginSuccess
import io.scalacraft.packets.clientbound.StatusPacket.{Pong, Response}
import io.scalacraft.packets.serverbound.HandshakingPackets.{Handshake, NextState}
import io.scalacraft.packets.serverbound.LoginPackets.LoginStart
import io.scalacraft.packets.serverbound.StatusPackets.{Ping, Request}

import scala.util.{Failure, Random, Success}

class UserContext(connectionManager: ConnectionManager, serverConfiguration: ServerConfiguration) extends Actor
  with ActorLogging with DefaultTimeout with ImplicitContext {

  private val world = context.actorSelection("/user/world")

  private var currentState: ConnectionState = ConnectionState.Handshaking
  private var player: ActorRef = _
  private var username: String = _
  private var uuid: UUID = _

  override def receive: Receive = handlePacketFor(handshakingBehaviour)

  private def handshakingBehaviour: Receive = {
    case Handshake(protocolVersion, _, _, NextState.Status) =>
      checkProtocolVersion(protocolVersion)
      currentState = ConnectionState.Status
      context.become(handlePacketFor(statusBehaviour))
      log.debug("An user starts handshaking process")
    case Handshake(protocolVersion, _, _, NextState.Login) =>
      checkProtocolVersion(protocolVersion)
      currentState = ConnectionState.Login
      context.become(handlePacketFor(loginBehaviour))
      log.debug("An user starts login process")
  }

  private def statusBehaviour: Receive = {
    case Request() => (world ? RequestOnlinePlayers).mapTo[Int] pipeTo self
    case onlinePlayers: Int =>
      log.debug("Request received. Sending server configuration..")
      writePacket(Response(serverConfiguration.loadConfiguration(onlinePlayers)))
      context.become(handlePacketFor {
        case Ping(payload) =>
          writePacket(Pong(payload))
          log.debug("Ping received. Sending pong and closing connection..")
      })
  }

  private def loginBehaviour: Receive = {
    case LoginStart(username) =>
      this.username = username
      log.info(s"User $username is authenticating..")

      world ? RegisterUser(username) onComplete {
        case Success(UserRegistered(entityId, uuid, playerRef)) =>
          player = playerRef
          this.uuid = uuid
          writePacket(LoginSuccess(uuid.toString, username))
          currentState = ConnectionState.Play
          context.become(handlePacketFor(playBehaviour))
          player ! RequestJoinGame(entityId, self)
          log.info(s"User $username with uuid $uuid authenticated successfully")
        case Success(_) => // never happens
        case Failure(ex) =>
          log.error(ex, s"Can't register user $username")
          stop()
      }
  }

  private def playBehaviour: Receive = {
    case message: Structure => player ! message
  }

  private def handlePacketFor(behaviour: Receive): Receive = {
    case message: Structure => writePacket(message)
    case RawPacket(packetId, buffer) =>
      val packetManager = Packets.serverboundPackageManagers(currentState)
      val message = packetManager.unmarshal(packetId)(buffer)
      if (serverConfiguration.debug) {
        log.info(s"C → S $message")
      }

      if (behaviour isDefinedAt message) {
        behaviour(message)
      } else {
        log.warning(s"Unhandled packet in state $currentState: $message")
      }
    case UserDisconnected =>
      if (currentState == ConnectionState.Play) {
        player ! RemovePlayer
        log.info(s"User $username with uuid $uuid disconnected")
      }
      stop()
  }

  private def writePacket(packet: Structure): Unit = {
    val packetManager = Packets.clientboundPackageManagers(currentState)
    connectionManager.writePacket(dataOutputStream => packetManager.marshal(packet)(dataOutputStream))

    if (serverConfiguration.debug) {
      log.info(s"S → C $packet")
    }
  }

  private def stop(): Unit = {
    connectionManager.closeConnection()
    self ! PoisonPill
  }

  private def checkProtocolVersion(clientProtocolVersion: Int): Unit = {
    // TODO: handle wrong protocol version
    if (clientProtocolVersion != ServerConfiguration.VersionProtocol) {
      log.warning(s"Client use $clientProtocolVersion protocolVersion instead of ${ServerConfiguration.VersionProtocol}")
    }
  }

}

object UserContext {

  def props(connectionManager: ConnectionManager, serverConfiguration: ServerConfiguration): Props =
    Props(new UserContext(connectionManager, serverConfiguration))
  def name: String = s"UserContext-${Math.abs(new Random().nextInt())}"

}
