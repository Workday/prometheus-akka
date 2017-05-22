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

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.collection.JavaConverters._

import akka.actor._
import akka.dispatch.MessageDispatcher
import akka.testkit.TestProbe

class DispatcherMetricsSpec extends TestKitBaseSpec("DispatcherMetricsSpec") {

  sealed trait PoolType
  case object ForkJoinPoolType extends PoolType
  case object ThreadPoolType extends PoolType

  "the akka dispatcher metrics" should {
    "respect the configured include and exclude filters" in {
      val defaultDispatcher = forceInit(system.dispatchers.lookup("akka.actor.default-dispatcher"))
      val fjpDispatcher = forceInit(system.dispatchers.lookup("tracked-fjp"))
      val tpeDispatcher = forceInit(system.dispatchers.lookup("tracked-tpe"))
      val excludedDispatcher = forceInit(system.dispatchers.lookup("explicitly-excluded"))

      findDispatcherRecorder(defaultDispatcher.id, ForkJoinPoolType) shouldNot be(empty)
      findDispatcherRecorder(fjpDispatcher.id, ForkJoinPoolType) shouldNot be(empty)
      findDispatcherRecorder(tpeDispatcher.id, ThreadPoolType) shouldNot be(empty)
      findDispatcherRecorder(excludedDispatcher.id, ForkJoinPoolType) should be(empty)
    }
  }

  def findDispatcherRecorder(dispatcherName: String, poolType: PoolType): Map[String, Double] = {
    val metrics = poolType match {
      case ForkJoinPoolType => ForkJoinPoolMetrics.collect().asScala.toList
      case ThreadPoolType => ThreadPoolMetrics.collect().asScala.toList
    }
    val values = for(samples <- metrics;
      sample <- samples.samples.asScala if findUsingSuffix(sample.labelValues.asScala, dispatcherName))
      yield (sample.name, sample.value)
    values.toMap
  }

  def findUsingSuffix(list: Seq[String], suffix: String): Boolean = {
    list.find(v => v.endsWith(suffix)).isDefined
  }

  def forceInit(dispatcher: MessageDispatcher): MessageDispatcher = {
    val listener = TestProbe()
    Future {
      listener.ref ! "init done"
    }(dispatcher)
    listener.expectMsg("init done")

    dispatcher
  }
}
