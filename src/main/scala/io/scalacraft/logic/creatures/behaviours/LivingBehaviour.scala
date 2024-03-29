package io.scalacraft.logic.creatures.behaviours

import akka.actor.Actor
import io.scalacraft.loaders.Items
import io.scalacraft.logic.commons.Message.{EntityDead, SendToAll, UseEntityWithItem}
import io.scalacraft.logic.creatures.parameters.CreatureParameters
import io.scalacraft.logic.creatures.parameters.CreatureParameters.{SoundEffectPositionMultiplier, SoundPitch, SoundVolume}
import io.scalacraft.core.packets.Entities.Living
import io.scalacraft.core.packets.clientbound.PlayPackets._
import io.scalacraft.core.packets.serverbound.PlayPackets.{Attack, UseEntity}

/**
 * Used as a mixin to import the behaviour of a living entity actor.
 * More precisely, it handles player's attacks.
 * @tparam T the living entity instance (e.g Chicken, Sheep, ...)
 */

trait LivingBehaviour[T <: Living] extends BaseBehaviour[T] {
  this: CreatureParameters[T] with Actor =>

  val livingBehaviour: Receive = baseBehaviour orElse {
    case UseEntityWithItem(useEntity: UseEntity, itemId: Int) if useEntity.target == entityId => useEntity.useType match {
      case Attack() =>
        val damage = Items.getItemById(itemId).attackDamage
        val dead = inflictDamage(damage)
        world ! SendToAll(Animation(entityId, AnimationType.TakeDamage))
        world ! SendToAll(soundEffectFromId(hurtSoundEffectId))
        world ! SendToAll(EntityMetadata(entityId, metaData))
        if (dead) {
          world ! SendToAll(soundEffectFromId(deathSoundEffectId))
          context.parent ! EntityDead(entityId)
        }
      case _ => //ignored
    }
  }

  def hurtSoundEffectId: Int

  def deathSoundEffectId: Int

  def inflictDamage(damage: Float): Boolean = {
    metaData.health -= damage
    metaData.health <= 0
  }

  protected def soundEffectFromId(soundId: Int): SoundEffect =
    SoundEffect(soundId, SoundCategory.Master, posX * SoundEffectPositionMultiplier,
      posY * SoundEffectPositionMultiplier, posZ * SoundEffectPositionMultiplier, SoundVolume, SoundPitch)

}
