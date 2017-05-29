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

import scala.collection.JavaConverters._

import io.prometheus.client.Counter
import kamon.metric.Entity

object RouterMetrics {
  private val map = new java.util.concurrent.ConcurrentHashMap[Entity, RouterMetrics]().asScala
  def metricsFor(e: Entity) = map.getOrElseUpdate(e, new RouterMetrics(e))
  def hasMetricsFor(e: Entity) = map.contains(e)
}

class RouterMetrics(entity: Entity) {
  val actorName = metricFriendlyActorName(entity.name)
  val routingTime = Counter.build().name(s"akka_router_routing_time_$actorName").help("Akka Router routing time (Nanos)").register()
  val processingTime = Counter.build().name(s"akka_router_processing_time_$actorName").help("Akka Router processing time (Nanos)").register()
  val timeInMailbox = Counter.build().name(s"akka_router_time_in_mailbox_$actorName").help("Akka Router time in mailbox (Nanos)").register()
  val messages = Counter.build().name(s"akka_router_message_count_$actorName").help("Akka Router messages").register()
  val errors = Counter.build().name(s"akka_router_error_count_$actorName").help("Akka Router errors").register()
}
