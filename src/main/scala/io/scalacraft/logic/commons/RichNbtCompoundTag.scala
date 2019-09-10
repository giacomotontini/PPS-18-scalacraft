package io.scalacraft.logic.commons

import io.scalacraft.loaders.Blocks
import net.querz.nbt.CompoundTag

object RichNbtCompoundTag {

  /**
   * It's a nbt compound tag enriched with some methods useful to understand the block type
   * @param nbtTag the tag to be enriched
   */

  implicit class RichNbtCompoundTag(nbtTag: CompoundTag) {
    import Blocks._

    /**
     * Check if the block associated with the nbt tag is a water block
     * @return true if is a water block, false otherwise
     */
    def isWater: Boolean = nbtTag != null && blockFromCompoundTag(nbtTag).name.equals("water")

    /**
     * Check if the block associated with the nbt tag is a solid block
     * @return true if is a solid block, false otherwise
     */
    def isSolidBlock: Boolean = nbtTag != null &&
      List("solid_block", "block_entity").contains(blockFromCompoundTag(nbtTag).`type`.getOrElse(""))

    /**
     * Check if the block associated with the nbt tag is a spawnable block (i.e solid or water)
     * @return true if is a solid block, false otherwise
     */
    def isSpawnableSurface: Boolean = isSolidBlock || isWater

  }
}
