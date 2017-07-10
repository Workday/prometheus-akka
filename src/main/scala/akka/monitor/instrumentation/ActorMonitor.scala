/*
 * =========================================================================================
 * Copyright © 2017 Workday, Inc.
 * Copyright © 2013-2017 the kamon project <http://kamon.io/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 * =========================================================================================
 */
package akka.monitor.instrumentation

import org.aspectj.lang.ProceedingJoinPoint
import org.slf4j.LoggerFactory

import com.workday.prometheus.akka.{ ActorMetrics, ActorGroupMetrics, RouterMetrics }

import akka.actor.{ ActorRef, ActorSystem, Cell }
import akka.monitor.instrumentation.ActorMonitors.{ TrackedActor, TrackedRoutee }
import kamon.metric.Entity
import kamon.util.RelativeNanoTimestamp

trait ActorMonitor {
  def captureEnvelopeContext(): EnvelopeContext
  def processMessage(pjp: ProceedingJoinPoint, envelopeContext: EnvelopeContext): AnyRef
  def processFailure(failure: Throwable): Unit
  def cleanup(): Unit
}

object ActorMonitor {

  def createActorMonitor(cell: Cell, system: ActorSystem, ref: ActorRef, parent: ActorRef, actorCellCreation: Boolean): ActorMonitor = {
    val cellInfo = CellInfo.cellInfoFor(cell, system, ref, parent, actorCellCreation)

    if (cellInfo.isRouter)
      ActorMonitors.ContextPropagationOnly
    else {
      if (cellInfo.isRoutee && cellInfo.isTracked)
        createRouteeMonitor(cellInfo)
      else
        createRegularActorMonitor(cellInfo)
    }
  }

  def createRegularActorMonitor(cellInfo: CellInfo): ActorMonitor = {
    if (cellInfo.isTracked || cellInfo.trackingGroups.length > 0) {
      val actorMetrics = if (cellInfo.isTracked) Some(ActorMetrics.metricsFor(cellInfo.entity)) else None
      new TrackedActor(cellInfo.entity, actorMetrics, cellInfo.trackingGroups, cellInfo.actorCellCreation)
    } else {
      ActorMonitors.ContextPropagationOnly
    }
  }

  def createRouteeMonitor(cellInfo: CellInfo): ActorMonitor = {
    def routerMetrics = RouterMetrics.metricsFor(cellInfo.entity)
    new TrackedRoutee(cellInfo.entity, routerMetrics, cellInfo.trackingGroups, cellInfo.actorCellCreation)
  }
}

object ActorMonitors {

  val logger = LoggerFactory.getLogger(ActorMonitors.getClass)

  val ContextPropagationOnly = new ActorMonitor {
    def captureEnvelopeContext(): EnvelopeContext =
      EnvelopeContext(RelativeNanoTimestamp.now)

    def processMessage(pjp: ProceedingJoinPoint, envelopeContext: EnvelopeContext): AnyRef = {
      pjp.proceed()
    }

    def processFailure(failure: Throwable): Unit = {}
    def cleanup(): Unit = {}
  }

  class TrackedActor(val entity: Entity, actorMetrics: Option[ActorMetrics],
      trackingGroups: List[String], actorCellCreation: Boolean)
      extends GroupMetricsTrackingActor(entity, trackingGroups, actorCellCreation) {

    if (logger.isDebugEnabled()) {
      logger.debug(s"tracking ${entity.name} actor: ${actorMetrics.isDefined} actor-group: ${trackingGroups}")
    }

    override def captureEnvelopeContext(): EnvelopeContext = {
      actorMetrics.foreach { am =>
        am.mailboxSize.inc()
        am.messages.inc()
      }
      super.captureEnvelopeContext()
    }

    def processMessage(pjp: ProceedingJoinPoint, envelopeContext: EnvelopeContext): AnyRef = {
      val timestampBeforeProcessing = RelativeNanoTimestamp.now

      try {
        pjp.proceed()
      } finally {
        val timestampAfterProcessing = RelativeNanoTimestamp.now
        val timeInMailbox = timestampBeforeProcessing - envelopeContext.nanoTime
        val processingTime = timestampAfterProcessing - timestampBeforeProcessing

        actorMetrics.foreach { am =>
          am.processingTime.inc(processingTime.nanos)
          am.timeInMailbox.inc(timeInMailbox.nanos)
          am.mailboxSize.dec()
        }
        recordProcessMetrics(processingTime, timeInMailbox)
      }
    }

    override def processFailure(failure: Throwable): Unit = {
      actorMetrics.foreach { am =>
        am.errors.inc()
      }
      super.processFailure(failure)
    }
  }

  class TrackedRoutee(val entity: Entity, routerMetrics: RouterMetrics,
      trackingGroups: List[String], actorCellCreation: Boolean)
      extends GroupMetricsTrackingActor(entity, trackingGroups, actorCellCreation) {

    if (logger.isDebugEnabled()) {
      logger.debug(s"tracking ${entity.name} router: true actor-group: ${trackingGroups}")
    }

    override def captureEnvelopeContext(): EnvelopeContext = {
      routerMetrics.messages.inc()
      super.captureEnvelopeContext()
    }

    def processMessage(pjp: ProceedingJoinPoint, envelopeContext: EnvelopeContext): AnyRef = {
      val timestampBeforeProcessing = RelativeNanoTimestamp.now

      try {
        pjp.proceed()
      } finally {
        val timestampAfterProcessing = RelativeNanoTimestamp.now
        val timeInMailbox = timestampBeforeProcessing - envelopeContext.nanoTime
        val processingTime = timestampAfterProcessing - timestampBeforeProcessing

        routerMetrics.processingTime.inc(processingTime.nanos)
        routerMetrics.timeInMailbox.inc(timeInMailbox.nanos)
        recordProcessMetrics(processingTime, timeInMailbox)
      }
    }

    override def processFailure(failure: Throwable): Unit = {
      routerMetrics.errors.inc()
      super.processFailure(failure)
    }
  }

  abstract class GroupMetricsTrackingActor(entity: Entity,
      trackingGroups: List[String], actorCellCreation: Boolean) extends ActorMonitor {
    if (actorCellCreation) {
      trackingGroups.foreach { group =>
        ActorGroupMetrics.actorCount.labels(group).inc()
      }
    }

    def captureEnvelopeContext(): EnvelopeContext = {
      trackingGroups.foreach { group =>
        ActorGroupMetrics.mailboxSize.labels(group).inc()
        ActorGroupMetrics.messages.labels(group).inc()
      }
      EnvelopeContext(RelativeNanoTimestamp.now)
    }

    protected def recordProcessMetrics(processingTime: RelativeNanoTimestamp, timeInMailbox: RelativeNanoTimestamp): Unit = {
      trackingGroups.foreach { group =>
        ActorGroupMetrics.processingTime.labels(group).inc(processingTime.nanos)
        ActorGroupMetrics.timeInMailbox.labels(group).inc(timeInMailbox.nanos)
        ActorGroupMetrics.mailboxSize.labels(group).dec()
      }
    }

    def processFailure(failure: Throwable): Unit = {
      trackingGroups.foreach { group =>
        ActorGroupMetrics.errors.labels(group).inc()
      }
    }

    def cleanup(): Unit = {
      if (actorCellCreation) {
        trackingGroups.foreach { group =>
          ActorGroupMetrics.actorCount.labels(group).dec()
        }
      }
    }

  }
}
