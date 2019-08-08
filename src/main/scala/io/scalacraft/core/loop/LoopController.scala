package io.scalacraft.core.loop

import java.util.concurrent.ConcurrentLinkedQueue

import io.scalacraft.core.marshalling.Structure
import io.scalacraft.misc.ServerConfiguration

/*

object LoopController {

  private val SecondsToNano = 1000000000
  private val TickDelay = SecondsToNano / ServerConfiguration.TickRate

  private val packetsQueue = new ConcurrentLinkedQueue[Structure]()

  def start(): Unit = {
    var initialTime = System.nanoTime()
    var initialDeltaTime = System.nanoTime()
    var delta = 0d
    var frames = 0
    var ticks = 0
    var timer = System.currentTimeMillis()
    var step = 0

    while (true) {
      var currentTime = System.nanoTime()
      delta += (currentTime - initialTime) / TickDelay
      initialTime = currentTime

      if (delta >= 1) {

        initialDeltaTime = currentTime
        ticks/++
        delta--
        step++
      }

      if (deltaRender >= 1) {
        renderWindow.render(resource)

        frames++
        deltaRender--
      }

      if (System.currentTimeMillis() - timer > STATISTICS_EVERY_MILLISECONDS) {
        renderWindow.updateStatistics(ticks, frames)
        frames = 0
        ticks = 0
        timer += STATISTICS_EVERY_MILLISECONDS
      }
    }

    renderWindow.complete()
  }

  def enqueue(packet: Structure): Unit = packetsQueue.add(packet)



}

*/