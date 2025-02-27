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

package org.apache.cassandra.sidecar.config;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import io.vertx.core.net.SocketAddress;
import io.vertx.core.net.impl.SocketAddressImpl;
import org.apache.cassandra.sidecar.common.server.utils.MillisecondBoundConfiguration;
import org.apache.cassandra.sidecar.common.server.utils.MinuteBoundConfiguration;

/**
 * Configuration for the Sidecar Service and configuration of the REST endpoints in the service
 */
public interface ServiceConfiguration
{
    String SERVICE_POOL = "service";
    String INTERNAL_POOL = "internal";
    String HOST_ID = UUID.randomUUID().toString();

    /**
     * @return a unique identifier for the Sidecar instance
     */
    default String hostId()
    {
        return HOST_ID;
    }

    /**
     * @return Sidecar's HTTP REST API listen address
     */
    String host();

    /**
     * Returns a list of socket addresses where the Sidecar process will bind and listen for connections. Defaults to
     * the configured {@link #host()} and {@link #port()}.
     *
     * @return a list of socket addresses where Sidecar will listen
     */
    default List<SocketAddress> listenSocketAddresses()
    {
        return Collections.singletonList(
        new SocketAddressImpl(port(), Objects.requireNonNull(host(), "host must be provided")));
    }

    /**
     * @return Sidecar's HTTP REST API port
     */
    int port();

    /**
     * Determines if a connection will timeout and be closed if no data is received nor sent within the timeout.
     * Zero means don't timeout.
     *
     * @return the configured idle timeout value
     */
    MillisecondBoundConfiguration requestIdleTimeout();

    /**
     * @return the amount of time when a response is considered as timed-out after data has not been written
     */
    MillisecondBoundConfiguration requestTimeout();

    /**
     * @return {@code true} if TCP keep alive is enabled, {@code false} otherwise
     */
    boolean tcpKeepAlive();

    /**
     * @return the number of connections in the backlog that the incoming queue will hold
     */
    int acceptBacklog();

    /**
     * @return the maximum time skew allowed between the server and the client
     */
    MinuteBoundConfiguration allowableTimeSkew();

    /**
     * @return the number of vertx verticle instances that should be deployed
     */
    int serverVerticleInstances();

    /**
     * TODO: move operationalJob related configuration to its own class, when the number of configurable fields grows in the future
     * @return the size of the operational job tracker LRU cache
     */
    int operationalJobTrackerSize();

    /**
     * @return the max wait time for operational job to run internally before returning the http response;
     *         if the job finishes before the max wait time, it returns immediately on completion;
     *         otherwise, a response indicating the job is still running is returned after the max wait time.
     */
    MillisecondBoundConfiguration operationalJobExecutionMaxWaitTime();

    /**
     * @return the throttling configuration
     */
    ThrottleConfiguration throttleConfiguration();

    /**
     * @return the configuration for SSTable component uploads on this service
     */
    SSTableUploadConfiguration sstableUploadConfiguration();

    /**
     * @return the configuration for the SSTable Import functionality
     */
    SSTableImportConfiguration sstableImportConfiguration();

    /**
     * @return the configuration for the SSTable Snapshot functionality
     */
    SSTableSnapshotConfiguration sstableSnapshotConfiguration();

    /**
     * @return the configured worker pools for the service
     */
    Map<String, ? extends WorkerPoolConfiguration> workerPoolsConfiguration();

    /**
     * @return the configuration for the {@link #SERVICE_POOL}
     */
    default WorkerPoolConfiguration serverWorkerPoolConfiguration()
    {
        return workerPoolsConfiguration().get(SERVICE_POOL);
    }

    /**
     * @return the configuration for the {@link #INTERNAL_POOL}
     */
    default WorkerPoolConfiguration serverInternalWorkerPoolConfiguration()
    {
        return workerPoolsConfiguration().get(INTERNAL_POOL);
    }

    /**
     * @return the system-wide JMX configuration settings
     */
    JmxConfiguration jmxConfiguration();

    /**
     * @return the configuration for the global inbound and outbound traffic shaping options
     */
    TrafficShapingConfiguration trafficShapingConfiguration();

    /**
     * @return the configuration for sidecar schema
     */
    SchemaKeyspaceConfiguration schemaKeyspaceConfiguration();

    /**
     * @return the configuration for cdc
     */
    CdcConfiguration cdcConfiguration();

    /**
     * @return the configuration relevant to the coordination functionality of Sidecar
     */
    CoordinationConfiguration coordinationConfiguration();
}
