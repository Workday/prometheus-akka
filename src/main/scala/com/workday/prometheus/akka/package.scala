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
package com.workday.prometheus

import scala.annotation.tailrec

import io.prometheus.client.Collector

package object akka {
  def metricFriendlyActorName(actorPath: String) = {
    Collector.sanitizeMetricName(trimLeadingSlashes(actorPath).toLowerCase.replace("/", "_"))
  }

  @tailrec
  private def trimLeadingSlashes(s: String): String = {
    if (s.startsWith("/")) trimLeadingSlashes(s.substring(1)) else s
  }
}
