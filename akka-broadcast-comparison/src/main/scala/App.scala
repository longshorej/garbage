import akka.NotUsed
import akka.actor._
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent._
import scala.concurrent.duration._

object App {
  val Limit = 10000

  val WarmupTimes = 100
  val RunTimes = 1000

  case class Element(n: Int)

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem()
    implicit val mat = ActorMaterializer()


    println("************************")
    println(s"WARMUP: 2 * ${WarmupTimes}x")
    println("************************")

    timeAvgBroadcastHub(WarmupTimes)
    timeAvgPrefixAndTail(WarmupTimes)

    timeAvgBroadcastHub(WarmupTimes)
    timeAvgPrefixAndTail(WarmupTimes)


    println("************************")
    println(s"RUN: ${RunTimes}x")
    println("************************")

    timeAvgBroadcastHub(RunTimes)
    timeAvgPrefixAndTail(RunTimes)

    System.exit(0)

    def timeAvgBroadcastHub(n: Int): Unit = timeAvg("broadcastHub", n) {
      Await.result(broadcastHub().runWith(Sink.ignore), Duration.Inf)
    }

    def timeAvgPrefixAndTail(n: Int): Unit = timeAvg("prefixAndTail", n) {
      Await.result(prefixAndTail().runWith(Sink.ignore), Duration.Inf)
    }

    def broadcastHub() = {
      import system.dispatcher

      val (queue, source) = Source
        .queue[Element](1, OverflowStrategy.backpressure)
        .toMat(BroadcastHub.sink)(Keep.both)
        .run()

      limitInclusive
        .mapAsync(1)(i => queue.offer(Element(i)).filter(_ == QueueOfferResult.Enqueued))
        .runWith(Sink.ignore)
        .onComplete(_ => queue.complete())

      source
    }

    def prefixAndTail() = {
      import system.dispatcher

      val (queue, source) = Source
        .queue[Element](1, OverflowStrategy.backpressure)
        .prefixAndTail(0)
        .map(_._2)
        .toMat(Sink.head)(Keep.both)
        .run()

      limitInclusive
        .mapAsync(1)(i => queue.offer(Element(i)).filter(_ == QueueOfferResult.Enqueued))
        .runWith(Sink.ignore)
        .onComplete(_ => queue.complete())

        Source.fromFutureSource(source).mapMaterializedValue(_ => NotUsed)
    }

    def limitInclusive =
      Source
        .repeat(0)
        .scan(0)((a, _) => a + 1)
        .takeWhile(_ <= Limit)
  }

  def timeAvg[A](name: String, avg: Int)(f: => A) = {
    val s = System.nanoTime

    for (i <- 1.to(avg)) f

    println(s"[$name] time (averaged over $avg times): ${(System.nanoTime - s) / 1e6 / avg}ms")
  }
}

