package io.scalacraft.logic.traits.creatures

import akka.actor.Actor
import io.scalacraft.logic.commons.Message.{EntityDead, SendToAll, UseEntityWithItem}
import io.scalacraft.packets.Entities.Living
import io.scalacraft.packets.clientbound.PlayPackets.{Animation, AnimationType, CombatEvent, DestroyEntities, Effect, EffectId, EntityMetadata, NamedSoundEffect, RemoveEntityEffect, SoundCategory, SoundEffect}
import io.scalacraft.packets.serverbound.PlayPackets.{Attack, UseEntity}

trait LivingBehaviour[T<: Living] extends BaseBehaviour[T] {
  this: CreatureParameters[T] with Actor =>

  val livingBehaviour: Receive = baseBehaviour orElse {
    case UseEntityWithItem(useEntity: UseEntity, itemId: Int) if useEntity.target == entityId=>  useEntity.useType match {
      case Attack() =>
        val damage = 1 //TODO use utils to determine damage based on item helded by user.
        val dead = inflictDamage(damage)
        world ! SendToAll(Animation(entityId, AnimationType.TakeDamage))
        world ! SendToAll(hurtSoundEffect())
        world ! SendToAll(EntityMetadata(entityId, metaData))
        if(dead) {
          world ! SendToAll(deathSoundEffect())
          context.parent ! EntityDead(entityId)
        }
      case _ => //ignored
    }
  }

  def hurtSoundEffect(): SoundEffect
  def deathSoundEffect(): SoundEffect

  def inflictDamage(damage: Float): Boolean = {
    metaData.health -= damage
    metaData.health <= 0
  }

}
