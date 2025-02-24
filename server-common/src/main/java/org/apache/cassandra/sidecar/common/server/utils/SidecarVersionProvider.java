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

package org.apache.cassandra.sidecar.common.server.utils;

import org.jetbrains.annotations.VisibleForTesting;

/**
 * Retrieves, caches, and provides build version of this Sidecar binary
 */
public class SidecarVersionProvider
{
    private final String sidecarVersion;

    @VisibleForTesting
    public SidecarVersionProvider()
    {
        this.sidecarVersion = "1.0-TEST";
    }

    public SidecarVersionProvider(String resource)
    {
        try
        {
            sidecarVersion = IOUtils.readFully(resource);
        }
        catch (Exception exception)
        {
            throw new IllegalStateException("Failed to retrieve Sidecar version from resource " + resource, exception);
        }
    }

    public String sidecarVersion()
    {
        return sidecarVersion;
    }
}
