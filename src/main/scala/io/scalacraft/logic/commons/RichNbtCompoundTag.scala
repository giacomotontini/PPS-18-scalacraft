package io.scalacraft.logic.commons

import net.querz.nbt.{CompoundTag, StringTag}

object RichNbtCompoundTag {

  implicit class RichNbtCompoundTag(nbtTag: CompoundTag) {
    val treeTypes = List("birch", "oak", "spruce")

    private[this] def checkIfIsASpecificTypeOfBlock(tagValue: String): Boolean = if (nbtTag != null) {
      val waterStringTag = new StringTag()
      waterStringTag.setValue(tagValue)
      nbtTag.entrySet().stream().anyMatch(_.getValue.equals(waterStringTag))
    }
    else false

    def isWater: Boolean = checkIfIsASpecificTypeOfBlock("minecraft:water")

    def isAir: Boolean = checkIfIsASpecificTypeOfBlock("minecraft:air") ||
      checkIfIsASpecificTypeOfBlock("minecraft:cave_air")

    def isGrass: Boolean = checkIfIsASpecificTypeOfBlock("minecraft:grass") ||
      checkIfIsASpecificTypeOfBlock("minecraft:tall_grass")

    def isLeaves: Boolean =
      treeTypes.exists(treeType => checkIfIsASpecificTypeOfBlock("minecraft:" + treeType + "_leaves"))

    def isWoodOfTree: Boolean =
      treeTypes.exists(treeType => checkIfIsASpecificTypeOfBlock("minecraft:" + treeType + "_log"))

    def isLava: Boolean = checkIfIsASpecificTypeOfBlock("minecraft:lava") ||
      checkIfIsASpecificTypeOfBlock("minecraft:flowing_lava")

    def isPoppy: Boolean = checkIfIsASpecificTypeOfBlock("minecraft:poppy")

    def isDandelion: Boolean = checkIfIsASpecificTypeOfBlock("minecraft:dandelion")

    def isOxyeDaisy: Boolean = checkIfIsASpecificTypeOfBlock("minecraft:oxeye_daisy")

    def isSpawnableSurface: Boolean =
      !isAir && !isWoodOfTree && !isLeaves && !isGrass && !isLava && !isPoppy && !isDandelion && !isWater && !isOxyeDaisy

    def areFlowers: Boolean = isDandelion || isGrass || isOxyeDaisy || isPoppy

    def emptyCube: Boolean = areFlowers || isAir || isGrass
  }

}
