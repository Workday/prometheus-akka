/*
 * =========================================================================================
 * Copyright © 2017,2018 Workday, Inc.
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

import org.slf4j.LoggerFactory

case class Entity(name: String, category: String, tags: Map[String, String]) {
  if(name == null) Entity.log.warn("Entity with name=null created (category: {}), your monitoring will not work as expected!", category)
}

object Entity {

  private lazy val log = LoggerFactory.getLogger(classOf[Entity])

  def apply(name: String, category: String): Entity =
    apply(name, category, Map.empty)

  def create(name: String, category: String): Entity =
    apply(name, category, Map.empty)

  def create(name: String, category: String, tags: Map[String, String]): Entity =
    new Entity(name, category, tags)
}