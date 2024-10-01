object RealApp {
  def timeAvg[A](avg: Int)(f: => A) = {
    val s = System.nanoTime
  
    for (i <- 1.to(avg)) f
  
    println("time (averaged over " + avg + " times): " + (System.nanoTime - s) / 1e6 / avg + "ms")
  }
  
  def main(args: Array[String]): Unit = {
    var acc = 0L
  
    println("running lazy val")
    timeAvg(1_000) { lazy val x = 42; var i = 0; while (i < 100_000_000) { acc += x + x + x; i += 1 } }
    println()

    println("running def")
    timeAvg(1_000) { def x = 42; var i = 0; while (i < 100_000_000) { acc += x + x + x; i += 1 } }
    println()

    println("running val")
    timeAvg(1_000) { val x = 42; var i = 0; while (i < 100_000_000) { acc += x + x + x; i += 1 } }
    println()

    println(s"accum is $acc")
  }

}

