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

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Configuration for this Sidecar process
 */
public interface SidecarConfiguration
{
    /**
     * @return a single configured cassandra instance
     * @deprecated in favor of configuring multiple instances in the yaml under cassandra_instances
     */
    InstanceConfiguration cassandra();

    /**
     * @return the configured Cassandra instances that this Sidecar manages
     */
    List<InstanceConfiguration> cassandraInstances();

    /**
     * @return the configuration of the REST Services
     */
    ServiceConfiguration serviceConfiguration();

    /**
     * @return the SSL configuration
     */
    SslConfiguration sslConfiguration();

    /**
     * @return configuration needed for setting up access control in sidecar.
     */
    AccessControlConfiguration accessControlConfiguration();

    /**
     * @return the configuration for the health check service
     */
    PeriodicTaskConfiguration healthCheckConfiguration();

    /**
     * @return configuration needed for metrics capture
     */
    MetricsConfiguration metricsConfiguration();

    /**
     * @return the configuration for Cassandra input validation
     */
    CassandraInputValidationConfiguration cassandraInputValidationConfiguration();

    /**
     * @return the Cassandra Driver parameters to use when connecting to the cluster
     */
    DriverConfiguration driverConfiguration();

    /**
     * @return the configuration for restore jobs done by sidecar
     */
    RestoreJobConfiguration restoreJobConfiguration();

    /**
     * @return the configuration for Amazon S3 client
     */
    S3ClientConfiguration s3ClientConfiguration();

    /**
     * @return the configuration for vert.x
     */
    @Nullable VertxConfiguration vertxConfiguration();

    /**
     * @return the configuration for Schema Reporting
     */
    @NotNull
    SchemaReportingConfiguration schemaReportingConfiguration();
}
