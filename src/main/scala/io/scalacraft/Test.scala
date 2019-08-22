package io.scalacraft


case object Test extends App {

  val test1 = Range(5, 10).inclusive
  println(test1.last)
  println(test1.end)

  val test2 = Range(5, 10)
  println(test2.last)
  println(test2.end)
}
