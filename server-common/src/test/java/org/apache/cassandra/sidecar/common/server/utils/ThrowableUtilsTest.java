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

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import org.apache.cassandra.sidecar.common.server.ThrowingRunnable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ThrowableUtilsTest
{
    /**
     * A new type of checked exception to throw for testing
     */
    private static class CheckedException extends Exception
    {
    }

    @Test
    @SuppressWarnings("Convert2MethodRef")
    void testThrowingSupplier()
    {
        Supplier<Void> supplier = ThrowableUtils.supplier(() ->
        {
            throw new CheckedException();
        });

        assertThatThrownBy(() -> supplier.get())
            .isInstanceOf(Exception.class)
            .hasCauseInstanceOf(CheckedException.class);
    }

    @Test
    void testThrowingConsumer()
    {
        Consumer<Void> consumer = ThrowableUtils.consumer(object ->
        {
            throw new CheckedException();
        });

        assertThatThrownBy(() -> consumer.accept(null))
            .isInstanceOf(Exception.class)
            .hasCauseInstanceOf(CheckedException.class);
    }

    @Test
    void testThrowingFunction()
    {
        Function<Void, Void> supplier = ThrowableUtils.function(object ->
        {
            throw new CheckedException();
        });

        assertThatThrownBy(() -> supplier.apply(null))
            .isInstanceOf(Exception.class)
            .hasCauseInstanceOf(CheckedException.class);
    }

    @Test
    void testGetCause()
    {
        Exception testEx = new IllegalStateException(new RuntimeException());
        Exception ex = new RuntimeException(testEx);
        assertThat(ThrowableUtils.getCause(ex, IllegalStateException.class)).isSameAs(testEx);
        assertThat(ThrowableUtils.getCause(ex, RuntimeException.class)).isSameAs(ex);
        assertThat(ThrowableUtils.getCause(ex, NoSuchFieldError.class)).isNull();
        assertThat(ThrowableUtils.getCause(null, RuntimeException.class)).isNull();
        assertThat(ThrowableUtils.getCause(new RuntimeException(), NoSuchFieldError.class)).isNull();
    }

    @Test
    void testGetCauseWithCircularRef()
    {
        Exception root = new IOException();
        Exception testEx = new IllegalStateException(root);
        Exception ex = new RuntimeException(testEx);
        root.initCause(ex); // create a circular chain

        // The invocations of ThrowableUtils.getCause print the warning "Circular exception reference detected!"
        assertThat(ThrowableUtils.getCause(ex, IllegalStateException.class)).isSameAs(testEx);
        assertThat(ThrowableUtils.getCause(ex, RuntimeException.class)).isSameAs(ex);
        // The exception is not found. The tracing loop should remain finite and exit with null.
        assertThat(ThrowableUtils.getCause(ex, NoSuchFieldError.class)).isNull();
    }

    @Test
    void testGetCauseWithPredicate()
    {
        Exception inner = new RuntimeException("inner exception");
        Exception testEx = new IllegalStateException(inner);
        Exception ex = new RuntimeException("outer exception", testEx);
        assertThat(ThrowableUtils.getCause(ex, cause -> cause instanceof RuntimeException
                                                        && cause.getMessage().equals("inner exception")))
        .isSameAs(inner);

        assertThat(ThrowableUtils.getCause(ex, cause -> cause instanceof RuntimeException
                                                        && cause.getMessage().equals("outer exception")))
        .isSameAs(ex);

        assertThat(ThrowableUtils.getCause(ex, cause -> cause instanceof RuntimeException
                                                        && cause.getMessage().equals("non-existing exception")))
        .isNull();
    }

    @Test
    void testPropagate()
    {
        Callable<String> callable = () -> {
            throw new IOException("fail to perform I/O");
        };
        assertThatThrownBy(() -> ThrowableUtils.propagate(callable))
        .isExactlyInstanceOf(RuntimeException.class)
        .hasCauseExactlyInstanceOf(IOException.class);

        ThrowingRunnable runnable = () -> {
            throw new IOException("fail to perform I/O");
        };
        assertThatThrownBy(() -> ThrowableUtils.propagate(runnable))
        .isExactlyInstanceOf(RuntimeException.class)
        .hasCauseExactlyInstanceOf(IOException.class);
    }
}
