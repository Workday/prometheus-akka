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
import akka.monitor.instrumentation.CellInfo
import akka.testkit.TestProbe
import kamon.metric.Entity

class ActorMetricsSpec extends TestKitBaseSpec("ActorMetricsSpec") {

  import ActorMetricsTestActor._

  "the actor metrics" should {
    "respect the configured include and exclude filters" in {
      val trackedActor = createTestActor("tracked-actor")
      val nonTrackedActor = createTestActor("non-tracked-actor")
      val excludedTrackedActor = createTestActor("tracked-explicitly-excluded-actor")

      actorMetricsRecorderOf(trackedActor) should not be empty
      actorMetricsRecorderOf(nonTrackedActor) shouldBe empty
      actorMetricsRecorderOf(excludedTrackedActor) shouldBe empty

      val metrics = actorMetricsRecorderOf(trackedActor).get
      metrics.actorName shouldEqual "actormetricsspec_user_tracked_actor"
      metrics.messages.get shouldEqual 1.0
    }
  }

  def actorMetricsRecorderOf(ref: ActorRef): Option[ActorMetrics] = {
    val name = CellInfo.cellName(system, ref)
    val entity = Entity(name, MetricsConfig.Actor)
    if (ActorMetrics.hasMetricsFor(entity)) {
      Some(ActorMetrics.metricsFor(entity))
    } else {
      None
    }
  }

  def createTestActor(name: String): ActorRef = {
    val actor = system.actorOf(Props[ActorMetricsTestActor], name)
    val initialiseListener = TestProbe()

    // Ensure that the router has been created before returning.
    actor.tell(Ping, initialiseListener.ref)
    initialiseListener.expectMsg(Pong)

    actor
  }
}
