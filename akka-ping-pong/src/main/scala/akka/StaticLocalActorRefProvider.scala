package akka

import akka.actor._
import akka.event.EventStream

object StaticLocalActorRefProvider {
  def apply(    _systemName:   String,
                settings:      ActorSystem.Settings,
                eventStream:   EventStream,
                dynamicAccess: DynamicAccess): ActorRefProvider = {
    new LocalActorRefProvider(_systemName, settings, eventStream, dynamicAccess)
  }
}
