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

package org.apache.cassandra.sidecar.restore;

import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import com.google.common.util.concurrent.SidecarRateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.netty.handler.ssl.OpenSsl;
import org.apache.cassandra.sidecar.config.S3ClientConfiguration;
import org.apache.cassandra.sidecar.config.S3ProxyConfiguration;
import org.apache.cassandra.sidecar.config.SidecarConfiguration;
import org.apache.cassandra.sidecar.db.RestoreJob;
import org.apache.cassandra.sidecar.exceptions.RestoreJobFatalException;
import software.amazon.awssdk.core.client.config.SdkAdvancedAsyncClientOption;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.ProxyConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.utils.SdkAutoCloseable;
import software.amazon.awssdk.utils.ThreadFactoryBuilder;

/**
 * Manages a pool of {@link StorageClient}s
 */
@Singleton
public class StorageClientPool implements SdkAutoCloseable
{
    private static final Logger LOGGER = LoggerFactory.getLogger(StorageClientPool.class);

    private final Map<String, StorageClient> clientPool = new ConcurrentHashMap<>();
    private final Map<UUID, StorageClient> clientByJobId = new ConcurrentHashMap<>();
    private final ThreadPoolExecutor sharedExecutor;
    private final S3ClientConfiguration clientConfig;
    private final SidecarRateLimiter ingressFileRateLimiter;

    @Inject
    public StorageClientPool(SidecarConfiguration configuration,
                             @Named("IngressFileRateLimiter") SidecarRateLimiter ingressFileRateLimiter)
    {
        clientConfig = configuration.s3ClientConfiguration();
        this.ingressFileRateLimiter = ingressFileRateLimiter;
        sharedExecutor = new ThreadPoolExecutor(clientConfig.concurrency(), // core
                                                clientConfig.concurrency(), // max
                                                // keep alive
                                                clientConfig.threadKeepAlive().quantity(),
                                                clientConfig.threadKeepAlive().unit(),
                                                new LinkedBlockingQueue<>(), // unbounded work queue
                                                new ThreadFactoryBuilder()
                                                .threadNamePrefix(clientConfig.threadNamePrefix())
                                                .daemonThreads(true)
                                                .build());
        // Must set it to allow threads to time out, so that it can release resources when idle.
        sharedExecutor.allowCoreThreadTimeOut(true);
    }

    public StorageClient storageClient(RestoreJob restoreJob) throws RestoreJobFatalException
    {
        String region = restoreJob.secrets.readCredentials().region();
        StorageClient client = clientByJobId.computeIfAbsent(restoreJob.jobId, id -> storageClient(region));
        return client.authenticate(restoreJob);
    }

    /**
     * Revoke the credentials for the restore job that is identified by the id
     *
     * @param jobId id of the restore job
     */
    public void revokeCredentials(UUID jobId)
    {
        clientByJobId.computeIfPresent(jobId, (id, client) -> {
            client.revokeCredentials(id);
            return null;
        });
    }

    private StorageClient storageClient(String region)
    {
        return clientPool.computeIfAbsent(region, k -> {
            logIfOpenSslUnavailable();

            Map<SdkAdvancedAsyncClientOption<?>, ?> advancedOptions = Collections.singletonMap(
            SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR, sharedExecutor
            );
            Duration apiCallTimeout = Duration.ofMillis(clientConfig.apiCallTimeout().toMillis());
            S3AsyncClientBuilder clientBuilder =
            S3AsyncClient.builder()
                         .region(Region.of(region))
                         // Setting the same timeout for apiCall and apiCallAttempt; There is 1 attempt effectively, as we do retry in the application
                         .overrideConfiguration(b -> b.apiCallAttemptTimeout(apiCallTimeout)
                                                      .apiCallTimeout(apiCallTimeout))
                         .asyncConfiguration(b -> b.advancedOptions(advancedOptions));
            S3ProxyConfiguration s3ProxyConfiguration = clientConfig.proxyConfig();
            URI endpointOverride = s3ProxyConfiguration.endpointOverride();
            if (endpointOverride != null) // set for local testing only
                clientBuilder.endpointOverride(endpointOverride)
                             .forcePathStyle(true);

            NettyNioAsyncHttpClient.Builder nettyClientBuilder = NettyNioAsyncHttpClient.builder();
            S3ProxyConfiguration config = clientConfig.proxyConfig();
            if (config.isPresent())
            {
                ProxyConfiguration proxyConfig = ProxyConfiguration.builder()
                                                                   .host(config.proxy().getHost())
                                                                   .port(config.proxy().getPort())
                                                                   .scheme(config.proxy().getScheme())
                                                                   .username(config.username())
                                                                   .password(config.password())
                                                                   .build();
                nettyClientBuilder.proxyConfiguration(proxyConfig);
            }
            clientBuilder.httpClientBuilder(nettyClientBuilder);

            return new StorageClient(clientBuilder.build(), clientConfig.rangeGetObjectBytesSize(), ingressFileRateLimiter);
        });
    }

    @Override
    public void close()
    {
        clientPool.values().forEach(StorageClient::close);
        clientPool.clear();
        clientByJobId.clear();
    }

    private void logIfOpenSslUnavailable()
    {
        if (!OpenSsl.isAvailable())
        {
            LOGGER.info("OpenSSL is not available for S3AsyncClient");
        }
    }
}
