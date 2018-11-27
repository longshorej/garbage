import akka.StaticActorSystemImpl
import akka.actor._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object Pinger {
  case class Forward(to: ActorRef, message: Message)
  case class Message(count: Long)
  case class TalkTo(to: ActorRef)

  val Delay = 50.milliseconds
}

class Pinger extends Actor with ActorLogging with Timers {
  import Pinger._

  private implicit val ec: ExecutionContext = context.system.dispatcher

  override def receive: Receive = {
    case Forward(to, message) =>
      to ! message

    case Message(count) =>
      log.info(s"received $count")

      timers.startSingleTimer("updog", Forward(sender(), Message(count + 1L)), Delay)

    case TalkTo(to) =>
      to ! Message(0L)
  }
}

object App {
  def main(args: Array[String]): Unit = {
    val system = StaticActorSystemImpl("pinger-system")
    val pingerA = system.actorOf(Props[Pinger], "pingerA")
    val pingerB = system.actorOf(Props[Pinger], "pingerB")

    pingerA ! Pinger.TalkTo(pingerB)

    system.log.info("started actors")
  }
}

