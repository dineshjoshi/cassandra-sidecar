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

package org.apache.cassandra.sidecar.common.server;

import com.datastax.driver.core.Session;
import org.apache.cassandra.sidecar.exceptions.CassandraUnavailableException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A provider of a CQL Session. The session should be connected to:
 * <ul>
 *     <li>All locally-managed instances.</li>
 *     <li>At least one non-local instance. Preferably, at least two non-replica instances.</li>
 * </ul>
 */
public interface CQLSessionProvider
{
    /**
     * Provides a Session connected to the cluster; otherwise, it tries to connect to the cluster.
     * {@link CassandraUnavailableException} is thrown when no CQL connection can be established.
     *
     * @return the session that holds connections to a Cassandra cluster
     * @throws CassandraUnavailableException when CQL connection is not successful
     */
    @NotNull Session get() throws CassandraUnavailableException;

    /**
     * Gets the current Session object if it already exists.
     * Unlike {@link #get()}, it does not attempt to connect to the cluster,
     * and it can return {@code null} when no connection is established.
     * The call-sites are required to handle {@code null} value.
     *
     * @return the connected {@link Session} object if available. Null otherwise.
     */
    @Nullable Session getIfConnected();

    /**
     * Closes the CQLSessionProvider
     */
    void close();
}
