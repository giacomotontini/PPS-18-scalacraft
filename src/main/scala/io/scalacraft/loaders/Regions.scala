package io.scalacraft.loaders

import java.io.{File, IOException}

import net.querz.nbt.mca.{MCAFile, MCAUtil}

object Regions extends App {

  def loadRegions(): Map[(Int, Int), MCAFile] = {
    val regionsDir = new File("world/regions")
    val mcaRegex = ".*r\\.(-?\\d+)\\.(-?\\d+)\\.mca$".r

    if (!regionsDir.exists || !regionsDir.isDirectory) {
      throw new IOException("Cannot find regions files in world/regions")
    }

    regionsDir.listFiles filter(_.isFile) map { file => mcaRegex.findFirstMatchIn(file.getAbsolutePath) } collect {
      case Some(mca) => (mca.group(1).toInt, mca.group(2).toInt) -> MCAUtil.readMCAFile(mca.source.toString)
    } toMap
  }

}
