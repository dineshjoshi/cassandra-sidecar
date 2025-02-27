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

package org.apache.cassandra.sidecar.common.server.exceptions;

import org.apache.cassandra.sidecar.common.server.utils.ThrowableUtils;

/**
 * Exception thrown when an operational job conflict is detected
 */
public class OperationalJobException extends RuntimeException
{
    public static OperationalJobException wraps(Throwable throwable)
    {
        // extract the OperationalJobException from the stacktrace, if exists
        // otherwise, wraps the throwable as OperationalJobException
        OperationalJobException oje = ThrowableUtils.getCause(throwable, OperationalJobException.class);
        if (oje != null)
        {
            return oje;
        }
        else
        {
            return new OperationalJobException(throwable.getMessage(), throwable);
        }
    }

    public OperationalJobException(String message)
    {
        super(message);
    }

    public OperationalJobException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
