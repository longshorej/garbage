import akka.{actor => untyped}
import akka.actor.typed._
import akka.actor.typed.scaladsl._
import akka.actor.typed.scaladsl.adapter._
import akka.stream._
import akka.stream.scaladsl._
import scala.concurrent.Promise
import scala.concurrent.duration._

sealed trait Event

case class A(value: Unit) extends Event
case class B(value: Unit) extends Event
case class C(ref: ActorRef[Event], value: Promise[Unit]) extends Event

object As {
  def root(promiseCompletorRef: ActorRef[C]): Behavior[Event] = Behaviors.withTimers { timer =>
      Behaviors.receivePartial[Event] {
        case (context, A(_)) =>
          println("root got A")

          for (_ <- 0 until 1) {
            val actor = context.spawnAnonymous(stateOne(promiseCompletorRef))
            context.watch(actor)
          }

          Behaviors.same

        case (context, B(_)) =>
          Behaviors.same
    }.receiveSignal {
      case (context, _: Terminated) =>
        // *****************************************************************************                  <-------------------- ATTENTION
        // uncomment next line to repeatedly fail until ClassCastException, then exit                     <-------------------- ATTENTION
        // *****************************************************************************                  <-------------------- ATTENTION
        //context.self ! A(())

        Behaviors.same
    }
  }

  def promiseCompletor: Behavior[C] = Behaviors.receiveMessage {
    case C(ref, value) =>
      println("completing promise")
      value.success(())
      ref ! A(())

      Behaviors.same
  }

  def stateOne(promiseCompletorRef: ActorRef[C]): Behavior[Event] = Behaviors.setup { context =>
    import context.executionContext

    val promise = Promise[Unit]
    promiseCompletorRef ! C(context.self, Promise[Unit])

    promise.future.onComplete { _ =>
      println("Promise completed")

      context.self ! A(())
    }

    Behaviors.receiveMessagePartial[Event] {
      case A(()) =>
        println("stateOne got A")

        stateTwo(promiseCompletorRef)
    }
  }

  def stateTwo(promiseCompletorRef: ActorRef[C]): Behavior[Event] = Behaviors.withTimers { timer =>
    timer.startSingleTimer("timer", B(()), 1.millisecond)

    try {
      Behaviors.receiveMessagePartial[Event] {
        case A(()) =>
          println("stateTwo got A")
          Behaviors.stopped
        case B(()) =>
          println("stateTwo got B")

          throw new IllegalStateException

          Behaviors.same
      }.receiveSignal {
        case (context, PostStop) =>
          println("PostStop")
          Behaviors.same
      }
    } catch  { case e: ClassCastException =>
      // See L33-L38, the idea is to repeatedly try this condition until we fail, and then exit the JVM
      e.printStackTrace()
      System.exit(0)

      Behaviors.same
    }
  }
}

object App {
  def main(args: Array[String]): Unit = {
    implicit val system: untyped.ActorSystem = untyped.ActorSystem()

    implicit val mat: Materializer = ActorMaterializer()

    val promiseCompletorRef = system.spawnAnonymous(As.promiseCompletor)
    val root = system.spawn(As.root(promiseCompletorRef), "root")

    Source
      .single(())
      .via(Flow[Unit]
        .watch(root.toUntyped)
        .watchTermination() {
          case (_, terminated) =>
        }
        .map { _ =>
          root ! A(())

        }
        .withAttributes(ActorAttributes.supervisionStrategy {
          case _: IllegalStateException =>
            Supervision.Resume
        })
        .recoverWithRetries(-1, {
          case _: WatchedActorTerminatedException => Source.empty
        })
      )
      .runWith(Sink.ignore)
  }
}
