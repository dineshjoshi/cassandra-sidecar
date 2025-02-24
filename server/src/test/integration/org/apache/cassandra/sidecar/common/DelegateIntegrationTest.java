/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.cassandra.sidecar.common;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.impl.ConcurrentHashSet;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.apache.cassandra.distributed.UpgradeableCluster;
import org.apache.cassandra.distributed.api.Feature;
import org.apache.cassandra.distributed.api.IUpgradeableInstance;
import org.apache.cassandra.distributed.api.NodeToolResult;
import org.apache.cassandra.distributed.shared.ClusterUtils;
import org.apache.cassandra.sidecar.cluster.CassandraAdapterDelegate;
import org.apache.cassandra.sidecar.testing.IntegrationTestBase;
import org.apache.cassandra.sidecar.utils.SimpleCassandraVersion;
import org.apache.cassandra.testing.CassandraIntegrationTest;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpResponseStatus.SERVICE_UNAVAILABLE;
import static org.apache.cassandra.sidecar.server.SidecarServerEvents.ON_ALL_CASSANDRA_CQL_READY;
import static org.apache.cassandra.sidecar.server.SidecarServerEvents.ON_CASSANDRA_CQL_DISCONNECTED;
import static org.apache.cassandra.sidecar.server.SidecarServerEvents.ON_CASSANDRA_CQL_READY;
import static org.apache.cassandra.sidecar.server.SidecarServerEvents.ON_CASSANDRA_JMX_DISCONNECTED;
import static org.apache.cassandra.sidecar.server.SidecarServerEvents.ON_CASSANDRA_JMX_READY;
import static org.apache.cassandra.testing.utils.AssertionUtils.loopAssert;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ensures the Delegate works correctly
 */
@ExtendWith(VertxExtension.class)
class DelegateIntegrationTest extends IntegrationTestBase
{
    private final AtomicReference<Message<JsonObject>> allCassandraCqlReadyMessage = new AtomicReference<>();

    @BeforeEach
    void reset()
    {
        allCassandraCqlReadyMessage.set(null);
    }

    @Override
    protected void beforeServerStart()
    {
        vertx.eventBus().localConsumer(ON_ALL_CASSANDRA_CQL_READY.address(), allCassandraCqlReadyMessage::set);
    }

    @CassandraIntegrationTest()
    void testCorrectVersionIsEnabled()
    {
        CassandraAdapterDelegate delegate = sidecarTestContext.instancesMetadata()
                                                              .instanceFromId(1)
                                                              .delegate();
        assertThat(delegate).isNotNull();
        SimpleCassandraVersion version = delegate.version();
        assertThat(version).isNotNull();
        assertThat(version.major).isEqualTo(sidecarTestContext.version.major);
        assertThat(version.minor).isEqualTo(sidecarTestContext.version.minor);
        assertThat(version).isGreaterThanOrEqualTo(sidecarTestContext.version);
    }

    @CassandraIntegrationTest()
    void testHealthCheck(VertxTestContext context)
    {
        EventBus eventBus = vertx.eventBus();
        Checkpoint cqlReady = context.checkpoint();
        Checkpoint cqlDisconnected = context.checkpoint();

        CassandraAdapterDelegate adapterDelegate = sidecarTestContext.instancesMetadata()
                                                                     .instanceFromId(1)
                                                                     .delegate();
        assertThat(adapterDelegate).isNotNull();
        assertThat(adapterDelegate.isJmxUp()).as("jmx health check succeeds").isTrue();
        assertThat(adapterDelegate.isNativeUp()).as("native health check succeeds").isTrue();

        // Set up test listeners before disabling/enabling binary to avoid race conditions
        // where the event happens before the consumer is registered.
        eventBus.localConsumer(ON_CASSANDRA_CQL_DISCONNECTED.address(), (Message<JsonObject> message) -> {
            int instanceId = message.body().getInteger("cassandraInstanceId");
            CassandraAdapterDelegate delegate = sidecarTestContext.instancesMetadata()
                                                                  .instanceFromId(instanceId)
                                                                  .delegate();

            context.verify(() -> {
                assertThat(delegate).isNotNull();
                assertThat(delegate.isNativeUp()).as("health check fails after binary has been disabled").isFalse();
            });
            cqlDisconnected.flag();
            sidecarTestContext.cluster().get(1).nodetool("enablebinary");
        });

        eventBus.localConsumer(ON_CASSANDRA_CQL_READY.address(), (Message<JsonObject> reconnectMessage) -> {
            int instanceId = reconnectMessage.body().getInteger("cassandraInstanceId");
            CassandraAdapterDelegate delegate = sidecarTestContext.instancesMetadata()
                                                                  .instanceFromId(instanceId)
                                                                  .delegate();
            context.verify(() -> {
                assertThat(delegate).isNotNull();
                assertThat(delegate.isNativeUp()).as("health check succeeds after binary has been enabled")
                                                 .isTrue();
            });

            cqlReady.flag();
        });

        // Disable binary
        NodeToolResult nodetoolResult = sidecarTestContext.cluster().get(1).nodetoolResult("disablebinary");
        assertThat(nodetoolResult.getRc())
        .withFailMessage("Failed to disable binary:\nstdout:" + nodetoolResult.getStdout()
                         + "\nstderr: " + nodetoolResult.getStderr())
        .isEqualTo(0);
        // NOTE: enable binary happens inside the disable binary handler above, which then will trigger the
        // cqlReady flag.
    }

    @CassandraIntegrationTest(nodesPerDc = 3)
    void testAllInstancesHealthCheck()
    {
        Set<Integer> expectedCassandraInstanceIds = ImmutableSet.of(1, 2, 3);
        loopAssert(30, 1000, () -> {
            Message<JsonObject> message = allCassandraCqlReadyMessage.get();
            assertThat(message).isNotNull();
            JsonArray cassandraInstanceIds = message.body().getJsonArray("cassandraInstanceIds");
            assertThat(cassandraInstanceIds).hasSize(3);
            assertThat(IntStream.rangeClosed(1, cassandraInstanceIds.size()))
            .allMatch(expectedCassandraInstanceIds::contains);
        });
    }

    @CassandraIntegrationTest(nodesPerDc = 3)
    void testStoppingAnInstance(VertxTestContext context)
    {
        EventBus eventBus = vertx.eventBus();
        Checkpoint allCqlReady = context.checkpoint();
        Checkpoint cqlDisconnected = context.checkpoint();
        Checkpoint jmxDisconnected = context.checkpoint();

        Set<Integer> expectedCassandraInstanceIds = ImmutableSet.of(1, 2, 3);
        eventBus.localConsumer(ON_CASSANDRA_CQL_DISCONNECTED.address(), (Message<JsonObject> message) -> {
            context.verify(() -> {
                Integer instanceId = message.body().getInteger("cassandraInstanceId");
                assertThat(instanceId).isEqualTo(2);

                buildNativeHealthRequest(client, instanceId).send(assertHealthCheckNotOk(context, cqlDisconnected));
            });
        });

        eventBus.localConsumer(ON_CASSANDRA_JMX_DISCONNECTED.address(), (Message<JsonObject> message) -> {
            context.verify(() -> {
                Integer instanceId = message.body().getInteger("cassandraInstanceId");
                assertThat(instanceId).isEqualTo(2);

                buildJmxHealthRequest(client, instanceId).send(assertHealthCheckNotOk(context, jmxDisconnected));
            });
        });

        loopAssert(30, 1000, () -> {
            Message<JsonObject> message = allCassandraCqlReadyMessage.get();
            assertThat(message).isNotNull();
            JsonArray cassandraInstanceIds = message.body().getJsonArray("cassandraInstanceIds");
            assertThat(cassandraInstanceIds).hasSize(3);
            assertThat(IntStream.rangeClosed(1, cassandraInstanceIds.size()))
            .allMatch(expectedCassandraInstanceIds::contains);

            allCqlReady.flag();
            // Stop instance 2
            ClusterUtils.stopUnchecked(sidecarTestContext.cluster().get(2));
        });

    }

    @Timeout(value = 2, timeUnit = TimeUnit.MINUTES)
    @CassandraIntegrationTest(nodesPerDc = 2, newNodesPerDc = 1, startCluster = false)
    public void testChangingClusterSize(VertxTestContext context) throws InterruptedException
    {
        // assume the sidecar has 3 managed instances, even though the cluster only starts with 2 instances initially
        sidecarTestContext.setInstancesToManage(1, 2, 3);

        EventBus eventBus = vertx.eventBus();

        Checkpoint jmxConnected = context.checkpoint(3);
        Checkpoint nativeConnected = context.checkpoint(3);
        Checkpoint jmxNotConnected = context.checkpoint();
        Checkpoint nativeNotConnected = context.checkpoint();

        CountDownLatch firstTwoConnected = new CountDownLatch(2);

        Set<Integer> nativeConnectedInstances = new ConcurrentHashSet<>();
        Set<Integer> jmxConnectedInstances = new ConcurrentHashSet<>();

        eventBus.localConsumer(ON_CASSANDRA_JMX_READY.address(), (Message<JsonObject> message) -> {
            Integer instanceId = message.body().getInteger("cassandraInstanceId");
            logger.info("DBG: Received JMX connection notification for {}", instanceId);
            // make sure the instance wasn't already in the set before validating
            if (jmxConnectedInstances.add(instanceId))
            {
                jmxConnected.flag();
                validateJmxConnections(context, jmxConnectedInstances, jmxNotConnected, firstTwoConnected);
            }
        });

        eventBus.localConsumer(ON_CASSANDRA_CQL_READY.address(), (Message<JsonObject> message) -> {
            Integer instanceId = message.body().getInteger("cassandraInstanceId");
            logger.info("DBG: Received native connection notification for {}", instanceId);
            buildNativeHealthRequest(client, instanceId).send(assertHealthCheckOk(context, nativeConnected));
            // make sure the instance wasn't already in the set before validating/flagging
            if (nativeConnectedInstances.add(instanceId))
            {
                nativeConnected.flag();
                validateNativeConnections(context, nativeNotConnected, firstTwoConnected, nativeConnectedInstances);
            }
        });

        // Now that the event listeners are set up, start the cluster
        sidecarTestContext.cluster().startup();

        // Wait for the first two instances to get connected
        assertThat(firstTwoConnected.await(2, TimeUnit.MINUTES)).isTrue();

        // now start the 3rd instance - the test will complete when it's connected
        addNewInstance();
    }

    private void validateJmxConnections(VertxTestContext context, Set<Integer> jmxConnectedInstances,
                                        Checkpoint notOkCheckpoint, CountDownLatch firstTwoConnected)
    {
        int upInstanceCount = jmxConnectedInstances.size();
        if (upInstanceCount == 2)
        {
            buildJmxHealthRequest(client, 3).send(assertHealthCheckNotOk(context, notOkCheckpoint));
            logger.info("DBG: First two instances connected via JMX, third is down");
            firstTwoConnected.countDown();
        }
        else if (upInstanceCount == 3)
        {
            context.verify(() -> assertThat(jmxConnectedInstances).containsExactly(1, 2, 3));
        }
    }

    private void validateNativeConnections(VertxTestContext context,
                                           Checkpoint notOkCheckpoint,
                                           CountDownLatch firstTwoConnected,
                                           Set<Integer> nativeConnectedInstances)
    {
        int upInstanceCount = nativeConnectedInstances.size();
        if (upInstanceCount == 2)
        {
            context.verify(() -> assertThat(nativeConnectedInstances).containsExactly(1, 2));
            buildNativeHealthRequest(client, 3).send(assertHealthCheckNotOk(context, notOkCheckpoint));
            logger.info("DBG: First two instances connected via native, third is down");
            firstTwoConnected.countDown();
        }
        else if (upInstanceCount == 3)
        {
            context.verify(() -> assertThat(nativeConnectedInstances).containsExactly(1, 2, 3));
        }
    }

    private static Handler<AsyncResult<HttpResponse<Buffer>>> assertHealthCheckOk(VertxTestContext context,
                                                                                  Checkpoint checkpoint)
    {
        return context.succeeding(response -> context.verify(() -> {
            assertThat(response.statusCode()).isEqualTo(OK.code());
            assertThat(response.bodyAsJsonObject().getString("status")).isEqualTo("OK");
        }));
    }

    private Handler<AsyncResult<HttpResponse<Buffer>>> assertHealthCheckNotOk(VertxTestContext context,
                                                                              Checkpoint checkpoint)
    {
        return context.succeeding(response -> context.verify(() -> {
            assertThat(response.statusCode()).isEqualTo(SERVICE_UNAVAILABLE.code());
            assertThat(response.bodyAsJsonObject().getString("status")).isEqualTo("NOT_OK");
            checkpoint.flag();
        }));
    }

    private HttpRequest<Buffer> buildNativeHealthRequest(WebClient webClient, int instanceId)
    {
        return webClient.get(server.actualPort(),
                             "localhost",
                             "/api/v1/cassandra/native/__health?instanceId=" + instanceId);
    }

    private HttpRequest<Buffer> buildJmxHealthRequest(WebClient webClient, int instanceId)
    {
        return webClient.get(server.actualPort(),
                             "localhost",
                             "/api/v1/cassandra/jmx/__health?instanceId=" + instanceId);
    }

    private void addNewInstance()
    {
        UpgradeableCluster cluster = sidecarTestContext.cluster();
        IUpgradeableInstance newInstance = ClusterUtils.addInstance(cluster, cluster.get(1).config(), config -> {
            config.set("auto_bootstrap", true);
            config.with(Feature.GOSSIP,
                        Feature.JMX,
                        Feature.NATIVE_PROTOCOL);
        });
        newInstance.startup(cluster);
    }
}
