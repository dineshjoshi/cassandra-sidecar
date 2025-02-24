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

package org.apache.cassandra.sidecar.adapters.base;

import java.net.InetSocketAddress;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import org.apache.cassandra.sidecar.common.response.NodeSettings;
import org.apache.cassandra.sidecar.common.server.CQLSessionProvider;
import org.apache.cassandra.sidecar.common.server.ClusterMembershipOperations;
import org.apache.cassandra.sidecar.common.server.ICassandraAdapter;
import org.apache.cassandra.sidecar.common.server.JmxClient;
import org.apache.cassandra.sidecar.common.server.MetricsOperations;
import org.apache.cassandra.sidecar.common.server.StorageOperations;
import org.apache.cassandra.sidecar.common.server.TableOperations;
import org.apache.cassandra.sidecar.common.server.dns.DnsResolver;
import org.apache.cassandra.sidecar.common.server.utils.DriverUtils;
import org.apache.cassandra.sidecar.exceptions.CassandraUnavailableException;
import org.jetbrains.annotations.NotNull;

import static org.apache.cassandra.sidecar.exceptions.CassandraUnavailableException.Service.CQL;

/**
 * A {@link ICassandraAdapter} implementation for Cassandra 4.0 and later
 */
public class CassandraAdapter implements ICassandraAdapter
{
    protected final DnsResolver dnsResolver;
    protected final JmxClient jmxClient;
    protected final CQLSessionProvider cqlSessionProvider;
    protected final InetSocketAddress localNativeTransportAddress;
    protected final DriverUtils driverUtils;
    private volatile Host host;

    public CassandraAdapter(DnsResolver dnsResolver,
                            JmxClient jmxClient,
                            CQLSessionProvider cqlSessionProvider,
                            InetSocketAddress localNativeTransportAddress,
                            DriverUtils driverUtils)
    {
        this.dnsResolver = dnsResolver;
        this.jmxClient = jmxClient;
        this.cqlSessionProvider = cqlSessionProvider;
        this.localNativeTransportAddress = localNativeTransportAddress;
        this.driverUtils = driverUtils;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public Metadata metadata() throws CassandraUnavailableException
    {
        return cqlSessionProvider.get().getCluster().getMetadata();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public NodeSettings nodeSettings()
    {
        throw new UnsupportedOperationException("Node settings are not provided by this adapter");
    }

    @Override
    @NotNull
    public ResultSet executeLocal(Statement statement)
    {
        Session activeSession = cqlSessionProvider.get();
        Metadata metadata = metadata();
        Host host = getHost(metadata);
        statement.setConsistencyLevel(ConsistencyLevel.ONE);
        statement.setHost(host);
        return activeSession.execute(statement);
    }

    @Override
    @NotNull
    public InetSocketAddress localNativeTransportAddress()
    {
        return localNativeTransportAddress;
    }

    @Override
    @NotNull
    public InetSocketAddress localStorageBroadcastAddress()
    {
        Metadata metadata = metadata();
        return getHost(metadata).getBroadcastSocketAddress();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public StorageOperations storageOperations()
    {
        return new CassandraStorageOperations(jmxClient, dnsResolver);
    }

    @Override
    @NotNull
    public MetricsOperations metricsOperations()
    {
        return new CassandraMetricsOperations(jmxClient, cqlSessionProvider);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public ClusterMembershipOperations clusterMembershipOperations()
    {
        return new CassandraClusterMembershipOperations(jmxClient);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NotNull
    public TableOperations tableOperations()
    {
        return new CassandraTableOperations(jmxClient);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return "CassandraAdapter" + "@" + Integer.toHexString(hashCode());
    }

    @NotNull
    protected Host getHost(Metadata metadata)
    {
        if (host != null)
        {
            return host;
        }

        synchronized (this)
        {
            if (host == null)
            {
                host = driverUtils.getHost(metadata, localNativeTransportAddress);
                if (host == null)
                {
                    throw new CassandraUnavailableException(CQL, "No Host available in Metadata for address: " + localNativeTransportAddress);
                }
            }
        }
        return host;
    }
}
