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
package com.workday.prometheus.akka

import akka.actor._
import scala.concurrent.duration._

class ActorMetricsTestActor extends Actor {
  import ActorMetricsTestActor._

  override def receive = {
    case Discard ⇒
    case Fail    ⇒ throw new ArithmeticException("Division by zero.")
    case Ping    ⇒ sender ! Pong
    case TrackTimings(sendTimestamp, sleep) ⇒ {
      val dequeueTimestamp = System.nanoTime()
      sleep.map(s ⇒ Thread.sleep(s.toMillis))
      val afterReceiveTimestamp = System.nanoTime()

      sender ! TrackedTimings(sendTimestamp, dequeueTimestamp, afterReceiveTimestamp)
    }
  }
}

object ActorMetricsTestActor {
  case object Ping
  case object Pong
  case object Fail
  case object Discard

  case class TrackTimings(sendTimestamp: Long = System.nanoTime(), sleep: Option[Duration] = None)
  case class TrackedTimings(sendTimestamp: Long, dequeueTimestamp: Long, afterReceiveTimestamp: Long) {
    def approximateTimeInMailbox: Long = dequeueTimestamp - sendTimestamp
    def approximateProcessingTime: Long = afterReceiveTimestamp - dequeueTimestamp
  }
}
