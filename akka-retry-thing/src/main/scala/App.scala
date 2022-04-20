import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.Failure

object App {
  type Pixel = String

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem("akka-retry-thing")
    implicit val ec: ExecutionContext = system.dispatcher

    /* fires a single pixel and emits any failures */
    val fireStage = Flow[Pixel]
      .mapAsync(parallelism = 128)(pixel => fireAndForget(pixel).transformWith(res => Future.successful(pixel -> res)))
      .collect { case (pixel, Failure(_)) => pixel }

    val queue = Source.queue[Pixel](bufferSize = 1024)
      .via(fireStage)
      .delay(10.millis, DelayOverflowStrategy.backpressure)
      .via(fireStage) // first retry
      .delay(100.millis, DelayOverflowStrategy.backpressure)
      .via(fireStage) // second retry
      .delay(1000.millis, DelayOverflowStrategy.backpressure)
      .via(fireStage) // third retry
      .toMat(
        Sink.foreach { pixel =>
          // oh well
          
          println(s"failed after 3 retries, pixel=$pixel")
        }
      )(Keep.left)
      .run()

    // this would be run when handling an incoming request
    queue.offer("https://incomingpixel.com/") match {
      case QueueOfferResult.Enqueued =>
        println("returning 200")

      case QueueOfferResult.Dropped | QueueOfferResult.QueueClosed | QueueOfferResult.Failure(_) =>
        println("returning 503 or w/e")
    }
  }

  def fireAndForget(pixel: Pixel): Future[Unit] = Future.successful(()) // @TODO actually fire the pixel
}
