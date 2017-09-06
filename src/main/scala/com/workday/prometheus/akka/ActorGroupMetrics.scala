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

  private[akka] val MailboxMetricName = "akka_actor_group_mailboxes_size"
  private[akka] val ProcessingTimeMetricName = "akka_actor_group_processing_time"
  private[akka] val TimeInMailboxMetricName = "akka_actor_group_time_in_mailboxes"
  private[akka] val MessageCountMetricName = "akka_actor_group_message_count"
  private[akka] val ActorCountMetricName = "akka_actor_group_actor_count"
  private[akka] val ErrorCountMetricName = "akka_actor_group_error_count"

  val mailboxSize = Gauge.build().name(MailboxMetricName).help("Akka Actor Group mailboxes size").labelNames("groupName").register()
  val processingTime = Gauge.build().name(ProcessingTimeMetricName).help("Akka Actor Group processing time (Seconds)").labelNames("groupName").register()
  val timeInMailbox = Gauge.build().name(TimeInMailboxMetricName).help("Akka Actor Group time in mailboxes (Seconds)").labelNames("groupName").register()
  val messages = Counter.build().name(MessageCountMetricName).help("Akka Actor Group messages").labelNames("groupName").register()
  val actorCount = Gauge.build().name(ActorCountMetricName).help("Akka Actor Group actor count").labelNames("groupName").register()
  val errors = Counter.build().name(ErrorCountMetricName).help("Akka Actor Group errors").labelNames("groupName").register()
}
