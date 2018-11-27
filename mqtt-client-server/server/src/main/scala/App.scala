import akka.{Done, NotUsed}
import akka.actor._
import akka.stream._
import akka.stream.alpakka.mqtt.streaming._
import akka.stream.alpakka.mqtt.streaming.scaladsl.{ActorMqttClientSession, ActorMqttServerSession, Mqtt}
import akka.stream.scaladsl.{Flow, Keep, MergeHub, RestartFlow, Sink, Source, SourceQueueWithComplete, Tcp}
import akka.util.ByteString
import java.util.UUID

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}

object App {
  case class MqttEvent(event: Event[Promise[Unit]],
                       sourceQueue: SourceQueueWithComplete[Command[Promise[Unit]]])

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem("client")
    implicit val mat: Materializer = ActorMaterializer()
    implicit val ec: ExecutionContext = system.dispatcher

    val mqttSession = ActorMqttServerSession(MqttSessionSettings())

    val binding = Tcp().bind("localhost", 1884)

    var events = 0

    binding
      .flatMapMerge(1024, { conn =>
        println(s"connection: ${conn.remoteAddress}")

        val mqttFlow = Mqtt
          .serverSessionFlow[Promise[Unit]](mqttSession, ByteString(UUID.randomUUID().toString))
          .joinMat(
            conn.flow
              .recover {
                case _: Throwable => ByteString.empty
              }
              .watchTermination()(Keep.right)
          )(Keep.right)
          .map(Some.apply)
          .recover {
            case _: Throwable =>
              println(s"connection ended: ${conn.remoteAddress}")
              None
          }
          .collect {

            case Some(Right(event)) =>
              events += 1
              println(s"received #$events $event")
              event
          }

        val ((sourceQueue, connDone), publisher) = Source
          .queue[Command[Promise[Unit]]](32, OverflowStrategy.fail)
          .viaMat(mqttFlow)(Keep.both)
          .toMat(Sink.asPublisher(false))(Keep.both)
          .run()

        Source
          .fromPublisher(publisher)
          .map(MqttEvent(_, sourceQueue))
      })
      .foldAsync(()) { case (_, event) =>
        event match {
          case MqttEvent(Event(c: Connect, _), connection) =>
            val command = Command[Promise[Unit]](ConnAck(ConnAckFlags.None, ConnAckReturnCode.ConnectionAccepted))

            println(s"sending $command")

            connection
              .offer(Command(ConnAck(ConnAckFlags.None, ConnAckReturnCode.ConnectionAccepted)))
              .map(_ => ())

          case MqttEvent(Event(s: Subscribe, _), connection) if s.topicFilters.nonEmpty =>


            connection
              .offer(Command(SubAck(s.packetId, s.topicFilters.map(_._2))))
              .map { _ =>
                Source
                  .tick(0.seconds, 5.milliseconds, 1L)
                  .scan(0L)(_ + _)
                  .mapAsync(1) { count =>
                    val promise = Promise[Done]

                    val command = Command(Publish(s.topicFilters.head._1, ByteString(s"Testing #$count")), promise)

                    println(s"sending $command")

                    mqttSession ! command

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

                ()

              }

          case MqttEvent(Event(_: PubAck, Some(promise)), _) =>
            val _ = promise.success(())

            Future.successful(())

          case MqttEvent(Event(p: Publish, _), connection) if p.packetId.isDefined =>
            val command = Command[Promise[Unit]](PubAck(p.packetId.get))

            println(s"sending $command")

            connection
              .offer(command)
              .map(_ => ())

          case _ =>
            Future.successful(())
        }
      }
      .to(Sink.ignore)
      .run()
      .foreach { binding =>
        println(s"listening on ${binding.localAddress}")
      }
  }
}
