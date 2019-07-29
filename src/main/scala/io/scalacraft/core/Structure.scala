package io.scalacraft.core

trait Structure extends Product {

  override def toString: String = {
    val buffer = new StringBuilder
    buffer append this.productPrefix
    buffer append " { "
    this.productElementNames.zip(this.productIterator) foreach { tuple =>
      buffer append tuple._1
      buffer append " -> "
      buffer append tuple._2
      buffer append " | "
    }
    buffer.delete(buffer.length() - 3, buffer.length() - 1)
    buffer append "}\n"

    buffer.toString
  }
}
