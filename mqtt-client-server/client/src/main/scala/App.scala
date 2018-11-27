import akka.{Done, NotUsed}
import akka.actor._
import akka.stream._
import akka.stream.alpakka.mqtt.streaming._
import akka.stream.alpakka.mqtt.streaming.scaladsl.{ActorMqttClientSession, Mqtt}
import akka.stream.scaladsl.{Flow, Keep, MergeHub, Sink, Source, Tcp}
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
      .clientSessionFlow[Promise[Unit]](mqttSession)
      .join(
        Tcp()
          .outgoingConnection("localhost", 1884)
          .mapMaterializedValue(_ => NotUsed)
      )

    val clientId = UUID.randomUUID().toString

    var mqttSink: Sink[Command[Promise[Unit]], NotUsed] = null

    var events = 0

    mqttSink = MergeHub
        .source[Command[Promise[Unit]]]
        .map { command =>
          println(s"sending $command")
          command
        }
        .via(Flow[Command[Promise[Unit]]]
          .prepend(
            Source.single(
              Command[Promise[Unit]](Connect(clientId, ConnectFlags.None))
            )
          )
          .via(mqttFlow))
        .collect {
          case Right(event) =>
            events += 1
            println(s"received $events $event")
            event
        }
        .map {
          case event @ Event(_: ConnAck, _) =>
            Source
              .single(Command[Promise[Unit]](Subscribe(s"test-$clientId")))
              .runWith(mqttSink)

            event

          case event @ Event(_: PubAck, Some(promise)) =>
            val _ = promise.success(())

            event

          case event @ Event(p: Publish, _) if p.packetId.isDefined =>
            Source
              .single(Command[Promise[Unit]](PubAck(p.packetId.get)))
              .runWith(mqttSink)

            event
          case other =>
            other
        }
       .toMat(Sink.ignore)(Keep.left)
       .run()

      val publishTo = s"topic-${UUID.randomUUID()}"

    
      Source
        .tick(0.seconds, 5.milliseconds, 1L)
        .scan(0L)(_ + _)
        .mapAsync(1) { count =>
          val promise = Promise[Done]

          val publish = Publish(publishTo, ByteString(s"Testing #$count"))

          mqttSession ! Command(publish, promise)

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
