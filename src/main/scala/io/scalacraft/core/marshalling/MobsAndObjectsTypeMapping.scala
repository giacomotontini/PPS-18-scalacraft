package io.scalacraft.core.marshalling

import io.scalacraft.packets.Entities._

object MobsAndObjectsTypeMapping {
  val typeToMobEntityClass: Map[Int, Class[_]] = Map(
    0 -> classOf[AreaEffectCloud],
    1 -> classOf[ArmorStand],
    2 -> classOf[Arrow],
    3 -> classOf[Bat],
    4 -> classOf[Blaze],
    5 -> classOf[Boat],
    // 6 -> classOf[CaveSpider],
    7 -> classOf[Chicken],
    8 -> classOf[Cod],
    9 -> classOf[Cow],
    10 -> classOf[Creeper],
    11 -> classOf[Donkey],
    12 -> classOf[Dolphin],
    13 -> classOf[DragonFireball],
    14 -> classOf[Drowned],
    15 -> classOf[ElderGuardian],
    16 -> classOf[EnderCrystal],
    17 -> classOf[EnderDragon],
    18 -> classOf[Enderman],
    19 -> classOf[Endermite],
    20 -> classOf[EvocationFangs],
    21 -> classOf[EvocationIllager],
    // 22 -> classOf[XPOrb],
    // 23 -> classOf[EyeOfEnderSignal],
    // 24 -> classOf[FallingSand],
    // 25 -> classOf[FireworkRocketEntity],
    26 -> classOf[Ghast],
    // 27 -> classOf[Giant],
    28 -> classOf[Guardian],
    29 -> classOf[Horse],
    30 -> classOf[Husk],
    31 -> classOf[IllusionIllager],
    // 32 -> classOf[Item],
    33 -> classOf[ItemFrame],
    34 -> classOf[Fireball],
    //35 -> classOf[LeashKnot],
    36 -> classOf[Llama],
    37 -> classOf[LlamaSpit],
    // 38 -> classOf[LavaSlime],
    39 -> classOf[MinecartRideable],
    40 -> classOf[MinecartChest],
    41 -> classOf[MinecartCommandBlock],
    42 -> classOf[MinecartFurnace],
    43 -> classOf[MinecartHopper],
    44 -> classOf[MinecartSpawner],
    45 -> classOf[MinecartTNT],
    46 -> classOf[Mule],
    47 -> classOf[Mooshroom],
    48 -> classOf[Ocelot],
    // 49 -> classOf[Painting],
    50 -> classOf[Parrot],
    51 -> classOf[Pig],
    52 -> classOf[PufferFish],
    // 53 -> classOf[PigZombie],
    54 -> classOf[PolarBear],
    55 -> classOf[TntPrimed],
    56 -> classOf[Rabbit],
    57 -> classOf[Salmon],
    58 -> classOf[Sheep],
    59 -> classOf[Shulker],
    // 60 -> classOf[ShulkerBullet],
    61 -> classOf[Silverfish],
    62 -> classOf[Skeleton],
    63 -> classOf[SkeletonHorse],
    64 -> classOf[Slime],
    65 -> classOf[SmallFireball],
    66 -> classOf[Snowman],
    67 -> classOf[Snowball],
    // 68 -> classOf[SpectralArrow],
    69 -> classOf[Spider],
    70 -> classOf[Squid],
    71 -> classOf[Stray],
    72 -> classOf[TropicalFish],
    73 -> classOf[Turtle],
    74 -> classOf[Egg],
    75 -> classOf[EnderPearl],
    76 -> classOf[ExperienceBottle],
    77 -> classOf[Potion],
    78 -> classOf[Vex],
    79 -> classOf[Villager],
    80 -> classOf[IronGolem],
    81 -> classOf[VindicationIllager],
    82 -> classOf[Witch],
    // 83 -> classOf[WitherBoss],
    84 -> classOf[WitherSkeleton],
    85 -> classOf[WitherSkull],
    86 -> classOf[Wolf],
    87 -> classOf[Zombie],
    88 -> classOf[ZombieHorse],
    89 -> classOf[ZombieVillager],
    90 -> classOf[Phantom],
    // 91 -> classOf[LightningBolt],
    92 -> classOf[Player],
    // 93 -> classOf[FishingBobber],
    94 -> classOf[Trident]

  )
  val typeToObjectEntityClass: Map[Int, Class[_]] = Map(
    1 -> classOf[Boat],
    // 2 -> classOf[ItemStack]
    3 -> classOf[AreaEffectCloud],
    10 -> classOf[Minecart],
    // 50 -> classOf[ActivedTNT],
    51 -> classOf[EnderCrystal],
    60 -> classOf[TippedArrow],
    61 -> classOf[Snowball],
    62 -> classOf[Egg],
    63 -> classOf[Fireball],
    64 -> classOf[Blaze],
    65 -> classOf[EnderPearl],
    66 -> classOf[WitherSkull],
    67 -> classOf[Shulker],
    68 -> classOf[LlamaSpit],
    70 -> classOf[FallingBlock],
    71 -> classOf[ItemFrame],
    72 -> classOf[EyeOfEnder],
    73 -> classOf[Potion],
    75 -> classOf[ExperienceBottle],
    76 -> classOf[Firework],
    // 77 -> classOf[LeashKnot],
    78 -> classOf[ArmorStand],
    79 -> classOf[EvocationFangs],
    90 -> classOf[FishingHook],
    // 91 -> classOf[SpectralArrow],
    93 -> classOf[DragonFireball],
    94 -> classOf[Trident]
  )

  def fromTypeToObjectEntityClass(tpe: Int): Class[_] = {
    typeToObjectEntityClass(tpe)
  }

  def fromTypeToMobEntityClass(tpe: Int): Class[_] = {
    typeToMobEntityClass(tpe)
  }

  def fromObjectEntityClassToType(objectClass: Class[_]): Int = {
    val swappedMap = typeToObjectEntityClass.map(_.swap)
    swappedMap(objectClass)
  }

  def fromMobEntityClassToType(mobClass: Class[_]): Int = {
    val swappedMap = typeToMobEntityClass.map(_.swap)
    swappedMap(mobClass)
  }
}
