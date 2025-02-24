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

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.cassandra.sidecar.common.server.utils.SecondBoundConfiguration;
import org.apache.cassandra.sidecar.config.CdcConfiguration;

/**
 * Encapsulate configuration values for CDC
 */
public class CdcConfigurationImpl implements CdcConfiguration
{
    private static final Logger LOGGER = LoggerFactory.getLogger(CdcConfigurationImpl.class);
    public static final String SEGMENT_HARD_LINK_CACHE_EXPIRY_PROPERTY = "segment_hardlink_cache_expiry";
    public static final SecondBoundConfiguration DEFAULT_SEGMENT_HARD_LINK_CACHE_EXPIRY =
    SecondBoundConfiguration.parse("5m");

    protected SecondBoundConfiguration segmentHardLinkCacheExpiry;

    public CdcConfigurationImpl()
    {
        this.segmentHardLinkCacheExpiry = DEFAULT_SEGMENT_HARD_LINK_CACHE_EXPIRY;
    }

    public CdcConfigurationImpl(SecondBoundConfiguration segmentHardLinkCacheExpiry)
    {
        this.segmentHardLinkCacheExpiry = segmentHardLinkCacheExpiry;
    }

    @Override
    @JsonProperty(value = SEGMENT_HARD_LINK_CACHE_EXPIRY_PROPERTY)
    public SecondBoundConfiguration segmentHardLinkCacheExpiry()
    {
        return segmentHardLinkCacheExpiry;
    }

    @JsonProperty(value = SEGMENT_HARD_LINK_CACHE_EXPIRY_PROPERTY)
    public void setSegmentHardLinkCacheExpiry(SecondBoundConfiguration segmentHardlinkCacheExpiry)
    {
        this.segmentHardLinkCacheExpiry = segmentHardlinkCacheExpiry;
    }

    /**
     * Legacy property {@code segment_hardlink_cache_expiry_in_secs}
     *
     * @param segmentHardlinkCacheExpiryInSecs expiry in seconds
     * @deprecated in favor of {@code segment_hardlink_cache_expiry}
     */
    @JsonProperty(value = "segment_hardlink_cache_expiry_in_secs")
    @Deprecated
    public void setSegmentHardLinkCacheExpiryInSecs(long segmentHardlinkCacheExpiryInSecs)
    {
        LOGGER.warn("'segment_hardlink_cache_expiry_in_secs' is deprecated, use 'segment_hardlink_cache_expiry' instead");
        setSegmentHardLinkCacheExpiry(new SecondBoundConfiguration(segmentHardlinkCacheExpiryInSecs, TimeUnit.SECONDS));
    }
}
