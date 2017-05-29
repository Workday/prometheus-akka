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

import io.prometheus.client._

object ActorGroupMetrics {
  val mailboxSize = Gauge.build().name(s"akka_actor_group_mailboxes_size").help("Akka Actor Group mailboxes size").labelNames("groupName").register()
  val processingTime = Counter.build().name(s"akka_actor_group_processing_time").help("Akka Actor Group processing time (Nanos)").labelNames("groupName").register()
  val timeInMailbox = Counter.build().name(s"akka_actor_group_time_in_mailboxes").help("Akka Actor Group time in mailboxes (Nanos)").labelNames("groupName").register()
  val messages = Counter.build().name(s"akka_actor_group_message_count").help("Akka Actor Group messages").labelNames("groupName").register()
  val actorCount = Gauge.build().name(s"akka_actor_group_actor_count").help("Akka Actor Group actor count").labelNames("groupName").register()
  val errors = Counter.build().name(s"akka_actor_group_error_count").help("Akka Actor Group errors").labelNames("groupName").register()
}
