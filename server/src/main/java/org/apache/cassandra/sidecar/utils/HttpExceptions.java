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

package org.apache.cassandra.sidecar.utils;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.ext.web.handler.HttpException;
import org.apache.cassandra.sidecar.exceptions.CassandraUnavailableException;

/**
 * This class consists exclusively of static methods that operate on or return {@link Exception}s of type
 * {@link HttpException}. It contains convenience methods and wrappers to produce {@link HttpException} instances.
 */
public class HttpExceptions
{
    // Suppresses default constructor, ensuring non-instantiability.
    private HttpExceptions()
    {
    }

    /**
     * Convenience method that returns a {@link HttpException} with the provided {@link HttpResponseStatus status} and
     * {@code cause}
     *
     * @param status the {@link HttpResponseStatus}
     * @param cause  the cause
     * @return the {@link HttpException} with the provided parameters
     */
    public static HttpException wrapHttpException(HttpResponseStatus status, Throwable cause)
    {
        return wrapHttpException(status, null, cause);
    }

    /**
     * Convenience method that returns a {@link HttpException} with the provided {@link HttpResponseStatus status} and
     * {@code payload}.
     *
     * @param status  the {@link HttpResponseStatus}
     * @param payload the payload for the {@link HttpException}
     * @return the {@link HttpException} with the provided parameters
     */
    public static HttpException wrapHttpException(HttpResponseStatus status, String payload)
    {
        return wrapHttpException(status, payload, null);
    }

    /**
     * Convenience method that returns a {@link HttpException} with the provided {@link HttpResponseStatus status},
     * {@code payload}, and {@code cause}.
     *
     * @param status  the {@link HttpResponseStatus}
     * @param payload the payload for the {@link HttpException}
     * @param cause   the cause
     * @return the {@link HttpException} with the provided parameters
     */
    public static HttpException wrapHttpException(HttpResponseStatus status, String payload, Throwable cause)
    {
        if (cause instanceof HttpException)
        {
            return (HttpException) cause;
        }

        if (cause instanceof CassandraUnavailableException)
        {
            String actualPayload = payload == null ? cause.getMessage() : payload;
            return new HttpException(HttpResponseStatus.SERVICE_UNAVAILABLE.code(), actualPayload, cause);
        }

        if (payload != null)
        {
            return new HttpException(status.code(), payload, cause);
        }
        return new HttpException(status.code(), "Unexpected error encountered in handler", cause);
    }
}
