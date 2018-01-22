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
package com.workday.prometheus.akka.impl

import java.util.regex.Pattern

private[akka] case class EntityFilter(includes: List[PathFilter], excludes: List[PathFilter]) {
  def accept(name: String): Boolean =
    includes.exists(_.accept(name)) && !excludes.exists(_.accept(name))
}

private[akka] trait PathFilter {
  def accept(path: String): Boolean
}

private[akka] case class RegexPathFilter(path: String) extends PathFilter {
  private val pathRegex = path.r
  override def accept(path: String): Boolean = {
    path match {
      case pathRegex(_*) ⇒ true
      case _             ⇒ false
    }
  }
}

/**
  * Default implementation of PathFilter.  Uses glob based includes and excludes to determine whether to export.
  *
  * Get a regular expression pattern which accept any path names which match the given glob.  The glob patterns
  * function similarly to ant file patterns.
  *
  * See also: http://ant.apache.org/manual/dirtasks.html#patterns
  *
  * @author John E. Bailey
  * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
  */
private[akka] case class GlobPathFilter(glob: String) extends PathFilter {
  private val GLOB_PATTERN = Pattern.compile("(\\*\\*?)|(\\?)|(\\\\.)|(/+)|([^*?]+)")
  private val pattern = getGlobPattern(glob)

  def accept(path: String): Boolean = pattern.matcher(path).matches

  private def getGlobPattern(glob: String) = {
    val patternBuilder = new StringBuilder
    val m = GLOB_PATTERN.matcher(glob)
    var lastWasSlash = false
    while (m.find) {
      lastWasSlash = false
      val grp1 = m.group(1)
      if (grp1 != null) {
        // match a * or **
        if (grp1.length == 2) {
          // it's a *workers are able to process multiple metrics*
          patternBuilder.append(".*")
        }
        else { // it's a *
          patternBuilder.append("[^/]*")
        }
      }
      else if (m.group(2) != null) {
        // match a '?' glob pattern; any non-slash character
        patternBuilder.append("[^/]")
      }
      else if (m.group(3) != null) {
        // backslash-escaped value
        patternBuilder.append(Pattern.quote(m.group.substring(1)))
      }
      else if (m.group(4) != null) {
        // match any number of / chars
        patternBuilder.append("/+")
        lastWasSlash = true
      }
      else {
        // some other string
        patternBuilder.append(Pattern.quote(m.group))
      }
    }
    if (lastWasSlash) {
      // ends in /, append **
      patternBuilder.append(".*")
    }
    Pattern.compile(patternBuilder.toString)
  }
}