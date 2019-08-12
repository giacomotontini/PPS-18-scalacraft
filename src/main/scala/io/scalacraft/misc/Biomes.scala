package io.scalacraft.misc

object Biomes {

  sealed trait Biome {
    val id: Int
  }

  case object Ocean extends Biome {
    val id: Int = 0
  }

  case object DeepOcean extends Biome {
    val id: Int = 24
  }

  case object FrozenOcean extends Biome {
    val id: Int = 10
  }

  case object DeepFrozenOcean extends Biome {
    val id: Int = 50
  }

  case object ColdOcean extends Biome {
    val id: Int = 46
  }

  case object DeepColdOcean extends Biome {
    val id: Int = 49
  }

  case object LukewarmOcean extends Biome {
    val id: Int = 45
  }

  case object DeepLukewarmOcean extends Biome {
    val id: Int = 48
  }

  case object WarmOcean extends Biome {
    val id: Int = 44
  }

  case object DeepWarmOcean extends Biome {
    val id: Int = 47
  }

  case object River extends Biome {
    val id: Int = 7
  }

  case object FrozenRiver extends Biome {
    val id: Int = 11
  }

  case object Beach extends Biome {
    val id: Int = 16
  }

  case object StoneShore extends Biome {
    val id: Int = 25
  }

  case object SnowyBeach extends Biome {
    val id: Int = 26
  }

  case object Forest extends Biome {
    val id: Int = 4
  }

  case object WoodedHills extends Biome {
    val id: Int = 18
  }

  case object FlowerForest extends Biome {
    val id: Int = 132
  }

  case object BirchForest extends Biome {
    val id: Int = 27
  }

  case object BirchForestHills extends Biome {
    val id: Int = 28
  }

  case object TallBirchForest extends Biome {
    val id: Int = 155
  }

  case object TallBirchHills extends Biome {
    val id: Int = 156
  }

  case object DarkForest extends Biome {
    val id: Int = 29
  }

  case object DarkForestHills extends Biome {
    val id: Int = 157
  }

  case object Jungle extends Biome {
    val id: Int = 21
  }

  case object JungleHills extends Biome {
    val id: Int = 22
  }

  case object ModifiedJungle extends Biome {
    val id: Int = 149
  }

  case object JungleEdge extends Biome {
    val id: Int = 23
  }

  case object ModifiedJungleEdge extends Biome {
    val id: Int = 151
  }

  case object BambooJungle extends Biome {
    val id: Int = 168
  }

  case object BambooJungleHills extends Biome {
    val id: Int = 169
  }

  case object Taiga extends Biome {
    val id: Int = 5
  }

  case object TaigaHills extends Biome {
    val id: Int = 19
  }

  case object TaigaMountains extends Biome {
    val id: Int = 133
  }

  case object SnowyTaiga extends Biome {
    val id: Int = 30
  }

  case object SnowyTaigaHills extends Biome {
    val id: Int = 31
  }

  case object SnowyTaigaMountains extends Biome {
    val id: Int = 158
  }

  case object GiantTreeTaiga extends Biome {
    val id: Int = 32
  }

  case object GiantTreeTaigaHills extends Biome {
    val id: Int = 33
  }

  case object GiantSpruceTaiga extends Biome {
    val id: Int = 160
  }

  case object GiantSpruceTaigaHills extends Biome {
    val id: Int = 161
  }

  case object MushroomFields extends Biome {
    val id: Int = 14
  }

  case object MushroomFieldShore extends Biome {
    val id: Int = 15
  }

  case object Swamp extends Biome {
    val id: Int = 6
  }

  case object SwampHills extends Biome {
    val id: Int = 134
  }

  case object Savanna extends Biome {
    val id: Int = 35
  }

  case object SavannaPlateau extends Biome {
    val id: Int = 36
  }

  case object ShatteredSavanna extends Biome {
    val id: Int = 163
  }

  case object ShatteredSavannaPlateau extends Biome {
    val id: Int = 164
  }

  case object Plains extends Biome {
    val id: Int = 1
  }

  case object SunflowerPlains extends Biome {
    val id: Int = 129
  }

  case object Desert extends Biome {
    val id: Int = 2
  }

  case object DesertHills extends Biome {
    val id: Int = 17
  }

  case object DesertLakes extends Biome {
    val id: Int = 130
  }

  case object SnowyTundra extends Biome {
    val id: Int = 12
  }

  case object SnowyMountains extends Biome {
    val id: Int = 13
  }

  case object IceSpikes extends Biome {
    val id: Int = 140
  }

  case object Mountains extends Biome {
    val id: Int = 3
  }

  case object WoodedMountains extends Biome {
    val id: Int = 34
  }

  case object GravellyMountains extends Biome {
    val id: Int = 131
  }

  case object GravellyMountainsModified extends Biome {
    val id: Int = 162
  }

  case object MountainEdge extends Biome {
    val id: Int = 20
  }

  case object Badlands extends Biome {
    val id: Int = 37
  }

  case object BadlandsPlateau extends Biome {
    val id: Int = 39
  }

  case object ModifiedBadlandsPlateau extends Biome {
    val id: Int = 167
  }

  case object WoodedBadlandsPlateau extends Biome {
    val id: Int = 38
  }

  case object ModifiedWoodedBadlandsPlateau extends Biome {
    val id: Int = 166
  }

  case object ErodedBadlands extends Biome {
    val id: Int = 165
  }

  case object Nether extends Biome {
    val id: Int = 8
  }

  case object TheEnd extends Biome {
    val id: Int = 9
  }

  case object SmallEndIslands extends Biome {
    val id: Int = 40
  }

  case object EndMidlands extends Biome {
    val id: Int = 41
  }

  case object EndHighlands extends Biome {
    val id: Int = 42
  }

  case object EndBarrens extends Biome {
    val id: Int = 43
  }

  case object TheVoid extends Biome {
    val id: Int = 127
  }
}
