/*-
 * -\-\-
 * bigtable-autoscaler
 * --
 * Copyright (C) 2018 Spotify AB
 * --
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -/-/-
 */

package com.spotify.autoscaler;

public class ScalingEvent implements Comparable<ScalingEvent> {
  private int desiredNodeCount;
  private String reason;

  public ScalingEvent(final int desiredNodeCount, final String reason) {
    this.desiredNodeCount = desiredNodeCount;
    this.reason = reason;
  }

  public int getDesiredNodeCount() {
    return desiredNodeCount;
  }

  public String getReason() {
    return reason;
  }

  @Override
  public int compareTo(final ScalingEvent o) {
    return this.getDesiredNodeCount() > o.getDesiredNodeCount() ? 1 : -1;
  }
}
