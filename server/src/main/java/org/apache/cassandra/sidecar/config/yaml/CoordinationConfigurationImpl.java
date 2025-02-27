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

package org.apache.cassandra.sidecar.config.yaml;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.cassandra.sidecar.common.server.utils.MillisecondBoundConfiguration;
import org.apache.cassandra.sidecar.config.CoordinationConfiguration;
import org.apache.cassandra.sidecar.config.PeriodicTaskConfiguration;

/**
 * Configuration relevant to the coordination functionality of Sidecar
 */
public class CoordinationConfigurationImpl implements CoordinationConfiguration
{
    @JsonProperty("cluster_lease_claim")
    private final PeriodicTaskConfiguration clusterLeaseClaimConfiguration;

    public CoordinationConfigurationImpl()
    {
        this(new PeriodicTaskConfigurationImpl(true,
                                               MillisecondBoundConfiguration.parse("1s"),
                                               MillisecondBoundConfiguration.parse("1m")));
    }

    public CoordinationConfigurationImpl(PeriodicTaskConfiguration clusterLeaseClaimConfiguration)
    {
        this.clusterLeaseClaimConfiguration = clusterLeaseClaimConfiguration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @JsonProperty("cluster_lease_claim")
    public PeriodicTaskConfiguration clusterLeaseClaimConfiguration()
    {
        return clusterLeaseClaimConfiguration;
    }
}
