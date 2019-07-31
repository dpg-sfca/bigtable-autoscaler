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

package com.spotify.autoscaler.simulation;

import static org.junit.Assert.assertTrue;

import com.google.bigtable.admin.v2.Cluster;
import com.spotify.autoscaler.AutoscaleJobITBase;
import com.spotify.autoscaler.FakeBigtableCluster;
import com.spotify.autoscaler.FakeBigtableClusterLoader;
import com.spotify.autoscaler.TimeSupplier;
import com.spotify.autoscaler.db.BigtableCluster;
import com.spotify.autoscaler.db.BigtableClusterBuilder;
import java.io.IOException;
import java.time.Duration;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimulationIT extends AutoscaleJobITBase {

  private static final Logger logger = LoggerFactory.getLogger(SimulationIT.class);
  private static final double CRITICAL_ADDITIONAL_CPU_THRESHOLD = 0.3d;
  private static final double IDEAL_CPU_INTERVAL = 0.25d;

  private static final BigtableClusterBuilder DEFAULTS =
      new BigtableClusterBuilder().cpuTarget(0.6).maxNodes(2000).minNodes(3);

  public static class AllLoader implements ArgumentsProvider {

    @Override
    public Stream<? extends Arguments> provideArguments(final ExtensionContext extensionContext) {
      return FakeBigtableClusterLoader.all(SimulationIT.DEFAULTS).stream();
    }
  }

  @ParameterizedTest
  @ArgumentsSource(AllLoader.class)
  // @FromFile(Loader.simpleCluster())
  public void simulateCluster(final FakeBigtableCluster fakeBigtableCluster) throws IOException {
    setupMocksFor(fakeBigtableCluster);

    final TimeSupplier timeSupplier = (TimeSupplier) fakeBigtableCluster.getTimeSource();
    timeSupplier.setTime(fakeBigtableCluster.getFirstValidMetricsInstant());

    final BigtableCluster cluster = fakeBigtableCluster.getCluster();
    final int initialNodeCount = fakeBigtableCluster.getMetricsForNow().nodeCount().intValue();
    fakeBigtableCluster.setNumberOfNodes(initialNodeCount);
    bigtableInstanceClient.updateCluster(
        Cluster.newBuilder()
            .setName(cluster.clusterName())
            .setServeNodes(initialNodeCount)
            .build());

    testThroughTime(
        fakeBigtableCluster,
        timeSupplier,
        Duration.ofSeconds(60),
        1400,
        fakeBigtableCluster::getCPU,
        fakeBigtableCluster::getStorage,
        ignored ->
            assertTrue(fakeBigtableCluster.getCPU() < cluster.cpuTarget() + IDEAL_CPU_INTERVAL),
        ignored -> {
          logger.warn(
              "Instant: {}, cpu: {}, nodeCount: {}, status: {}",
              timeSupplier.get().toString(),
              fakeBigtableCluster.getCPU(),
              fakeBigtableCluster.getNumberOfNodes(),
              fakeBigtableCluster.getCPU() > cluster.cpuTarget() + CRITICAL_ADDITIONAL_CPU_THRESHOLD
                  ? "CRITICAL"
                  : fakeBigtableCluster.getCPU() > cluster.cpuTarget() ? "HIGH" : "NORMAL");

          assertTrue(
              fakeBigtableCluster.getCPU()
                  < cluster.cpuTarget() + CRITICAL_ADDITIONAL_CPU_THRESHOLD);
        });
  }
}