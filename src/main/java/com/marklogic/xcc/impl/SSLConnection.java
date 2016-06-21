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
import java.net.InetSocketAddress;
import java.nio.channels.ByteChannel;
import java.nio.channels.SocketChannel;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import com.marklogic.io.SslByteChannel;
import com.marklogic.xcc.SecurityOptions;
import com.marklogic.xcc.spi.ConnectionProvider;
import com.marklogic.xcc.spi.ServerConnection;

public class SSLConnection implements ServerConnection {
    private final ServerConnection plainConn;
    private final ConnectionProvider provider;
    private final ByteChannel sslChannel;

    public SSLConnection(ServerConnection conn, SecurityOptions securityOptions, SSLSocketPoolProvider provider,
            Logger logger) throws IOException {
        if (!(conn.channel() instanceof SocketChannel)) {
            throw new IllegalArgumentException("Underlying channel is not a SocketChannel");
        }

        // SocketChannel socketChannel = (SocketChannel)conn.channel();
        InetSocketAddress addr = provider.getAddress();
        SSLContext context = securityOptions.getSslContext();
        SSLEngine sslEngine = context.createSSLEngine(addr.getHostName(), addr.getPort());

        this.plainConn = conn;
        this.provider = provider;

//		socketChannel.configureBlocking (false);

        sslEngine.setUseClientMode(true);

        String[] protocols = securityOptions.getEnabledProtocols();
        if (protocols != null) {
            sslEngine.setEnabledProtocols(protocols);
        }

        String[] ciphers = securityOptions.getEnabledCipherSuites();
        if (ciphers != null) {
            sslEngine.setEnabledCipherSuites(ciphers);
        }

        sslChannel = new SslByteChannel(plainConn.channel(), sslEngine, logger);
    }

    public ByteChannel channel() {
        return sslChannel;
    }

    public ConnectionProvider provider() {
        return provider;
    }

    public long getTimeoutMillis() {
        return plainConn.getTimeoutMillis();
    }

    /**
     * @param timeoutMillis
     *            A duration, in milliseconds.
     * @deprecated Use {@link #setTimeoutTime(long)} instead.
     */
    @Deprecated
    public void setTimeoutMillis(long timeoutMillis) {
        plainConn.setTimeoutTime(timeoutMillis);
    }

    public long getTimeoutTime() {
        return plainConn.getTimeoutTime();
    }

    public void setTimeoutTime(long timeMillis) {
        plainConn.setTimeoutTime(timeMillis);
    }

    public void close() {
        try {
            sslChannel.close();
        } catch (IOException e) {
            // ignore
        }
    }

    public boolean isOpen() {
        // FIXME: finish this
        return plainConn.isOpen();
    }
}
