package scala.concurrent

import scala.concurrent.Future.InternalCallbackExecutor

object StaticInternalCallbackExecutor {
  val instance: InternalCallbackExecutor.type = InternalCallbackExecutor
}
