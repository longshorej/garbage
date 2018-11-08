import akka.{Done, NotUsed}
import akka.actor._
import akka.stream._
import akka.stream.alpakka.mqtt.streaming._
import akka.stream.alpakka.mqtt.streaming.scaladsl.{ActorMqttClientSession, Mqtt}
import akka.stream.scaladsl.{Flow, Keep, MergeHub, RestartFlow, Sink, Source, Tcp}
import akka.util.ByteString
import java.util.UUID
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Promise}
import scala.util.{Failure, Success}

object App {
  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem("client")
    implicit val mat: Materializer = ActorMaterializer()
    implicit val ec: ExecutionContext = system.dispatcher

    val mqttSession = ActorMqttClientSession(MqttSessionSettings())

    def mqttFlow =  Mqtt
      .clientSessionFlow[Promise[Done]](mqttSession)
      .join(
        Tcp()
          .outgoingConnection("localhost", 1882)
          .mapMaterializedValue(_ => NotUsed)
      )

    val clientId = UUID.randomUUID().toString

    val mqttWithRestartFlow = RestartFlow
      .withBackoff(
        minBackoff = 1.second,
        maxBackoff = 10.seconds,
        randomFactor = 0.10,
        maxRestarts = 60
      ) { () =>
        Flow[Command[Promise[Done]]]
          .prepend(
            Source.single(
              Command[Promise[Done]](Connect(clientId, ConnectFlags.None))
            )
          )
          .map { command =>
            println(s"sending $command")
            command
          }
          .via(mqttFlow)
      }

    val mqttSink =
      MergeHub
        .source[Command[Promise[Done]]]
        .via(mqttWithRestartFlow)
        .collect {
          case Right(event) =>
            event
        }
        .map {
          case event @ Event(_: PubAck, Some(promise)) =>
            println(s"received PubAck")
            val _ = promise.trySuccess(Done)
            event
          case other =>
            println(s"received $other")
            other
        }
       .toMat(Sink.ignore)(Keep.left)
       .run()

    Source
      .tick(0.seconds, 1.seconds, 1L)
      .scan(0L)(_ + _)
      .mapAsync(1) { count =>
        val promise = Promise[Done]

        val publish = Publish("test", ByteString(s"Testing #$count"))

        val _ = Source
          .single(Command(publish, promise))
          .runWith(mqttSink)

        promise.future
      }
      .runWith(Sink.ignore)
      .onComplete {
        case Success(_) =>
          System.exit(0)

        case Failure(t) =>
          t.printStackTrace()
          System.exit(1)
      }
  }
}
