/*
 * Copyright 2003-2016 MarkLogic Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.marklogic.xcc.impl;

import java.io.IOException;
import java.nio.channels.ByteChannel;

import com.marklogic.xcc.spi.ConnectionProvider;
import com.marklogic.xcc.spi.ServerConnection;

public class SimpleConnection implements ServerConnection {
    private final ByteChannel channel;
    private final ConnectionProvider provider;
    private long timeoutTime = 0;

    public SimpleConnection(ByteChannel channel, ConnectionProvider provider) {
        this.channel = channel;
        this.provider = provider;
    }

    public ByteChannel channel() {
        return channel;
    }

    public ConnectionProvider provider() {
        return provider;
    }

    public long getTimeoutMillis() {
        long millis = timeoutTime - System.currentTimeMillis();

        return (millis < 0) ? 0 : millis;
    }

    /**
     * Set timeout as a number of milliseconds in the future.
     * 
     * @param timeoutMillis
     *            A number of miliseconds.
     * @deprecated Use {@link #setTimeoutTime(long)} instead.
     */
    @Deprecated
    public void setTimeoutMillis(long timeoutMillis) {
        this.timeoutTime = System.currentTimeMillis() + timeoutMillis;
    }

    public long getTimeoutTime() {
        return timeoutTime;
    }

    public void setTimeoutTime(long timeMillis) {
        this.timeoutTime = timeMillis;
    }

    public void close() {
        try {
            channel.close();
        } catch (IOException e) {
            // ignore
        }
    }

    public boolean isOpen() {
        return channel.isOpen();
    }

    @Override
    public String toString() {
        return "SimpleConnection [provider: " + provider.toString() + "]";
    }
}
