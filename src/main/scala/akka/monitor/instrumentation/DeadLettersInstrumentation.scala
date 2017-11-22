package akka.monitor.instrumentation

import akka.actor.{ActorSystem, DeadLetter, UnhandledMessage}
import org.aspectj.lang.annotation.{After, Aspect, DeclareMixin, Pointcut}

import com.workday.prometheus.akka.{DeadLetterMetrics, metricFriendlyName}

trait HasSystem {
  def system: ActorSystem
  def setSystem(system: ActorSystem): Unit
}

object HasSystem {
  def apply(): HasSystem = new HasSystem {
    private var _system: ActorSystem = _

    override def system: ActorSystem = _system

    override def setSystem(system: ActorSystem): Unit = _system = system
  }
}

@Aspect
class DeadLettersInstrumentation {

  @DeclareMixin("akka.event.EventStream+")
  def mixinHasSystem: HasSystem = HasSystem()

  @Pointcut("execution(akka.event.EventStream.new(..)) && this(eventStream) && args(system, debug)")
  def eventStreamCreation(eventStream: HasSystem, system: ActorSystem, debug: Boolean): Unit = {}

  @After("eventStreamCreation(eventStream, system, debug)")
  def aroundEventStreamCreation(eventStream: HasSystem, system:ActorSystem, debug: Boolean): Unit = {
    eventStream.setSystem(system)
  }


  @Pointcut("execution(* akka.event.EventStream.publish(..)) && this(stream) && args(event)")
  def streamPublish(stream: HasSystem, event: AnyRef): Unit = {}

  @After("streamPublish(stream, event)")
  def afterStreamSubchannel(stream: HasSystem, event: AnyRef): Unit = trackEvent(stream, event)

  private def trackEvent(stream: HasSystem, event: AnyRef): Unit = event match {
    case dl: DeadLetter => DeadLetterMetrics.deadLetters.labels(metricFriendlyName(stream.system.name)).inc()
    case um: UnhandledMessage => DeadLetterMetrics.unhandledMessages.labels(stream.system.name).inc()
    case _ => ()
  }

}