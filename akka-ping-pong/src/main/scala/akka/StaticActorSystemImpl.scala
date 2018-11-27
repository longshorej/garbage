package akka

import java.util.concurrent.ThreadFactory

import akka.actor.ActorSystem.{Settings, findClassLoader}
import akka.actor.{ActorSystem, ActorSystemImpl, BootstrapSetup, DynamicAccess, Props}
import akka.actor.setup.ActorSystemSetup
import akka.dispatch.{BoundedMailbox, BoundedNodeMessageQueue, UnboundedControlAwareMailbox, UnboundedMailbox}
import akka.event.{DefaultLoggingFilter, EventStream, Logging, LoggingAdapter}
import akka.event.Logging.LogLevel
import akka.routing.{ConsistentHashingPool, RoundRobinPool}
import com.typesafe.config.{Config, ConfigFactory}

import scala.collection.immutable
import scala.concurrent.{ExecutionContext, StaticInternalCallbackExecutor}
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

object StaticActorSystemImpl {
  def apply(name: String): ActorSystem = {
    val setup = ActorSystemSetup(BootstrapSetup(None, None, None))
    val bootstrapSettings = setup.get[BootstrapSetup]
    val cl = bootstrapSettings.flatMap(_.classLoader).getOrElse(findClassLoader())
    val appConfig = bootstrapSettings.flatMap(_.config).getOrElse(ConfigFactory.load(cl))
    val defaultEC = bootstrapSettings.flatMap(_.defaultExecutionContext)

    new StaticActorSystemImpl(name, appConfig, cl, defaultEC, None, setup).start()
  }
}

/**
 * Totally production ready!
 */
class StaticActorSystemImpl(override val name: String,
                            applicationConfig: Config,
                            classLoader: ClassLoader,
                            defaultExecutionContext: Option[ExecutionContext],
                            override val guardianProps: Option[Props],
                            setup: ActorSystemSetup) extends ActorSystemImpl(name, applicationConfig, classLoader, defaultExecutionContext, guardianProps, setup) {
  import StaticActorSystemImpl._

  private val cl = classLoader

  private val logAdapter = new DefaultLoggingFilter(() => Logging.InfoLevel)

  override def createDynamicAccess(): DynamicAccess = new DynamicAccess {
    override def createInstanceFor[T](clazz: Class[_], args: immutable.Seq[(Class[_], AnyRef)])(implicit evidence$1: ClassTag[T]): Try[T] = {
      println(s"createInstanceFor2($clazz, ${args.map(_._2)})")

      null
    }

    override def getClassFor[T](fqcn: String)(implicit evidence$2: ClassTag[T]): Try[Class[_ <: T]] = {
      fqcn match {
        case "akka.dispatch.BoundedMessageQueueSemantics" =>
          Success(classOf[BoundedMailbox].asInstanceOf[Class[T]])
        case "akka.dispatch.MultipleConsumerSemantics" =>
          Success(classOf[UnboundedMailbox].asInstanceOf[Class[T]])
        case "akka.dispatch.BoundedControlAwareMessageQueueSemantics" =>
          Success(classOf[UnboundedControlAwareMailbox].asInstanceOf[Class[T]])
        case "akka.event.LoggerMessageQueueSemantics" =>
          Success(classOf[akka.event.LoggerMailboxType].asInstanceOf[Class[T]])
        case "akka.dispatch.BoundedDequeBasedMessageQueueSemantics" =>
          Success(classOf[akka.dispatch.UnboundedDequeBasedMailbox].asInstanceOf[Class[T]])
        case "akka.dispatch.UnboundedMessageQueueSemantics" =>
          Success(classOf[akka.dispatch.UnboundedMailbox].asInstanceOf[Class[T]])
        case "akka.dispatch.UnboundedDequeBasedMessageQueueSemantics" =>
          Success(classOf[akka.dispatch.UnboundedDequeBasedMailbox].asInstanceOf[Class[T]])
        case "akka.dispatch.UnboundedControlAwareMessageQueueSemantics" =>
          Success(classOf[akka.dispatch.UnboundedControlAwareMailbox].asInstanceOf[Class[T]])
        case "akka.dispatch.ControlAwareMessageQueueSemantics" =>
          Success(classOf[akka.dispatch.UnboundedControlAwareMailbox].asInstanceOf[Class[T]])
        case "akka.dispatch.DequeBasedMessageQueueSemantics" =>
          Success(classOf[akka.dispatch.UnboundedDequeBasedMailbox].asInstanceOf[Class[T]])
        case "akka.event.Logging$DefaultLogger" =>
          Success(classOf[akka.event.Logging.DefaultLogger].asInstanceOf[Class[T]])


        case other =>
          Failure(new IllegalArgumentException(s"getClassFor cannot instantiate $other"))
      }
    }

    override def createInstanceFor[T](fqcn: String, args: immutable.Seq[(Class[_], AnyRef)])(implicit evidence$3: ClassTag[T]): Try[T] = {
      fqcn match {
        case "akka.event.DefaultLoggingFilter" =>
          Success(logAdapter.asInstanceOf[T])
        case "akka.actor.LightArrayRevolverScheduler" =>
          Success(
            new akka.actor.LightArrayRevolverScheduler(
              args(0)._2.asInstanceOf[Config],
              args(1)._2.asInstanceOf[LoggingAdapter],
              args(2)._2.asInstanceOf[ThreadFactory]
            ).asInstanceOf[T]
          )
        case "akka.actor.LocalActorRefProvider" =>
          Success(
            StaticLocalActorRefProvider.apply(
              args(0)._2.asInstanceOf[String],
              args(1)._2.asInstanceOf[ActorSystem.Settings],
              args(2)._2.asInstanceOf[EventStream],
              args(3)._2.asInstanceOf[DynamicAccess]
            ).asInstanceOf[T]
          )

        case "akka.routing.ConsistentHashingPool" =>
          println(args.map(_._1))
          Success(
            new ConsistentHashingPool(args(0)._2.asInstanceOf[Config]).asInstanceOf[T]
          )

        case "akka.routing.RoundRobinPool" =>
          Success(
            new RoundRobinPool(args(0)._2.asInstanceOf[Config]).asInstanceOf[T]
          )

        case "akka.dispatch.BoundedMessageQueueSemantics" =>
          Success(
            new akka.dispatch.UnboundedMailbox(args(0)._2.asInstanceOf[Settings], applicationConfig)
              .asInstanceOf[T]
          )
        case "akka.dispatch.UnboundedMailbox" =>
          Success(
            new akka.dispatch.UnboundedMailbox(args(0)._2.asInstanceOf[Settings], args(1)._2.asInstanceOf[Config])
              .asInstanceOf[T]
          )

        case "akka.actor.DefaultSupervisorStrategy" =>
          Success(
            (new akka.actor.DefaultSupervisorStrategy).asInstanceOf[T]
          )

        case other =>
          println(args.map(_._1))
          Failure(new IllegalArgumentException(s"createInstanceFor cannot instantiate $other"))
      }
    }

    override def getObjectFor[T](fqcn: String)(implicit evidence$4: ClassTag[T]): Try[T] = {
      println(s"getObjectFor($fqcn)")

      fqcn match {
        case "scala.concurrent.Future$InternalCallbackExecutor$" =>
          Success(StaticInternalCallbackExecutor.instance.asInstanceOf[T])

        case other =>
          Failure(new IllegalArgumentException(s"getObjectFor cannot instantiate $other"))
      }

    }

    override def classLoader: ClassLoader = {
      cl
    }
  }
}
