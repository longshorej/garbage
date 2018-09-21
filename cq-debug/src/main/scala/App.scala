

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.cisco.streambed.durablequeue.chroniclequeue.ChronicleQueue
import java.nio.file.Paths

import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import com.cisco.streambed.durablequeue.DurableQueue.{CommandRequest, Send}

import scala.concurrent.duration._

sealed trait Mode

case object ProducerMode extends Mode
case object TransformerMode extends Mode
case object ConsumerMode extends Mode

object App {
  def logic(mode: Mode): Unit = {
    implicit val system = ActorSystem("cq-debug")
    implicit val mat = ActorMaterializer()
    import system.dispatcher

    val queue = ChronicleQueue.queue(basePath = Paths.get("/tmp/cq-debug"),
      keepAlive = 10.seconds,
      readPollInterval = 10.milliseconds,
      readerWriterAskTimeout = 5.seconds)

    val topic1 = "test1"
    val topic2 = "test2"

    mode match {
      case ProducerMode =>
        Source
          .tick(0.millis, 100.milliseconds, ())
          .scan(0L) { case (accum, _) => accum + 1L }
          .map(accum => CommandRequest(Send(0L, ByteString(s"test $accum"), topic1)))
          .via(queue.flow)
          .runWith(Sink.ignore)
          .onComplete { t =>
            println(t)
            System.exit(1)
          }

      case TransformerMode =>
        queue
          .source(topic1)
          .map(received => CommandRequest(Send(0L, received.data, topic2)))
          .via(queue.flow)
          .runWith(Sink.ignore)
          .onComplete { t =>
            println(t)
            System.exit(1)
          }

      case ConsumerMode =>
        queue
          .source(topic2)
          .map(item => item.data.utf8String)
          .runForeach { value =>
            println(value)
          }
          .onComplete { t =>
            println(t)
            System.exit(1)
          }


    }
  }
}

object Producer {
  def main(args: Array[String]): Unit = App.logic(ProducerMode)
}

object Transformer {
  def main(args: Array[String]): Unit = App.logic(TransformerMode)
}

object Consumer {
  def main(args: Array[String]): Unit = App.logic(ConsumerMode)
}