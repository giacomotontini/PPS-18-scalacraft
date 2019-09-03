package io.scalacraft.logic.traits.creatures

import akka.actor.Actor
import io.scalacraft.logic.commons.Message.{SendToAll, UseEntityWithItem}
import io.scalacraft.packets.Entities.Living
import io.scalacraft.packets.clientbound.PlayPackets.{Animation, AnimationType, DestroyEntities, Effect, EffectId, EntityMetadata, NamedSoundEffect, SoundCategory, SoundEffect}
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
        if(dead) {
          world ! SendToAll(DestroyEntities(List(entityId)))
          world ! SendToAll(deathSoundEffect())
          context stop self //TODO: tell the world (or creature spawner) to kill me
        } else {
          world ! SendToAll(EntityMetadata(entityId, metaData))
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
