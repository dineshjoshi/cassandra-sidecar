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

package org.apache.cassandra.sidecar.db.schema;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.Session;
import org.apache.cassandra.sidecar.config.SchemaKeyspaceConfiguration;
import org.apache.cassandra.sidecar.config.SidecarConfiguration;
import org.jetbrains.annotations.NotNull;

/**
 * Manages table setup needed for features provided by Sidecar. For e.g. creates schema needed for
 * {@link org.apache.cassandra.sidecar.db.RestoreJob} table and {@link org.apache.cassandra.sidecar.db.RestoreSlice}
 * table
 */
public class SidecarInternalKeyspace extends AbstractSchema
{
    private final SchemaKeyspaceConfiguration keyspaceConfig;
    private final boolean isEnabled;
    private final Set<TableSchema> tableSchemas = ConcurrentHashMap.newKeySet();

    public SidecarInternalKeyspace(SidecarConfiguration config)
    {
        this.keyspaceConfig = config.serviceConfiguration().schemaKeyspaceConfiguration();
        this.isEnabled = keyspaceConfig.isEnabled();
    }

    public void registerTableSchema(TableSchema schema)
    {
        if (!isEnabled)
        {
            logger.warn("Sidecar schema is disabled!");
            return;
        }

        tableSchemas.add(schema);
    }

    @Override
    protected void prepareStatements(@NotNull Session session)
    {
    }

    @Override
    protected boolean exists(@NotNull Metadata metadata)
    {
        return metadata.getKeyspace(keyspaceName()) != null;
    }

    @Override
    protected boolean initializeInternal(@NotNull Session session,
                                         @NotNull Predicate<AbstractSchema> shouldCreateSchema)
    {
        super.initializeInternal(session, shouldCreateSchema);

        boolean initialized = true;
        for (AbstractSchema schema : tableSchemas)
        {
            // Attempts to initialize all schemas.
            // Sets initialized to false if any of the schema initialization fails
            initialized = schema.initialize(session, shouldCreateSchema) && initialized;
        }

        return initialized;
    }

    @Override
    protected String keyspaceName()
    {
        return keyspaceConfig.keyspace();
    }

    @Override
    protected String createSchemaStatement()
    {
        return String.format("CREATE KEYSPACE IF NOT EXISTS %s WITH REPLICATION = %s",
                             keyspaceName(), keyspaceConfig.createReplicationStrategyString());
    }
}
