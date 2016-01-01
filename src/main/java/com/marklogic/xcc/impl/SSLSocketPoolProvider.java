/*
 * Copyright 2003-2015 MarkLogic Corporation
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
import java.net.SocketAddress;
import java.nio.channels.ByteChannel;
import java.nio.channels.SocketChannel;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.marklogic.io.ResourcePool;
import com.marklogic.io.SslByteChannel;
import com.marklogic.xcc.Request;
import com.marklogic.xcc.SecurityOptions;
import com.marklogic.xcc.Session;
import com.marklogic.xcc.spi.ConnectionErrorAction;
import com.marklogic.xcc.spi.ConnectionProvider;
import com.marklogic.xcc.spi.ServerConnection;
import com.marklogic.xcc.spi.SingleHostAddress;

public class SSLSocketPoolProvider implements ConnectionProvider, SingleHostAddress {
    private final SocketAddress address;
    private final SecurityOptions securityOptions;
    private final SocketPoolProvider socketProvider;
    private final ResourcePool<SocketAddress, ServerConnection> sslPool;
    private final Logger logger;

    public SSLSocketPoolProvider(SocketAddress address, SecurityOptions options) throws NoSuchAlgorithmException,
            KeyManagementException {
        logger = Logger.getLogger(ConnectionProvider.class.getName());

        logger.fine("constructing new SSLSocketPoolProvider");

        this.address = address;
        this.socketProvider = new SocketPoolProvider(address);
        this.securityOptions = options;

        sslPool = new ResourcePool<SocketAddress, ServerConnection>();
    }

	@Override
	public boolean equals(Object o) {
		if(this == o) return true;
		if(o == null) return false;
		if(!(o instanceof SSLSocketPoolProvider)) return false;
		return address.equals(((SSLSocketPoolProvider)o).getAddress()) &&
			securityOptions.equals(((SSLSocketPoolProvider)o).getSecurityOptions());
	}

	@Override
	public int hashCode() {
		return address.hashCode() + securityOptions.hashCode();
	}

    // -----------------------------------------------------------
    // Impl of SingleHostAddress interface

    public InetSocketAddress getAddress() {
        return (InetSocketAddress)((address instanceof InetSocketAddress) ? address : null);
    }

	public SecurityOptions getSecurityOptions() {
		return securityOptions;
	}

    // -----------------------------------------------------------
    // Impl of ConnectionProvider interface

    public ServerConnection obtainConnection(Session session, Request request, Logger logger) throws IOException {
        ServerConnection conn = sslPool.get(address);

        if (conn != null) {
            return conn;
        }

        conn = socketProvider.obtainConnection(session, request, logger);

        return new SSLConnection(conn, securityOptions, this, logger);
    }

    public void returnConnection(ServerConnection connection, Logger logger) {
        if (getLogger(logger).isLoggable(Level.FINE)) {
            getLogger(logger).fine("returnConnection for " + address + ", expire=" + connection.getTimeoutMillis());
        }

        ByteChannel channel = connection.channel();

        if ((channel == null) || (!(channel instanceof SslByteChannel))) {
            getLogger(logger).fine("channel is not eligible for pooling, dropping");
            try {
                channel.close();
            } catch (IOException e) {
                getLogger(logger).fine("unable to close channel");
            }
            return;
        }

        SslByteChannel socketChannel = (SslByteChannel)channel;

        if (!socketChannel.isOpen()) {
            getLogger(logger).fine("channel has been closed, dropping");
            return;
        }

        long timeoutMillis = connection.getTimeoutMillis();

        if (timeoutMillis <= 0) {
            getLogger(logger).fine("channel has already expired, closing");

            connection.close();

            return;
        }

        long timeoutTime = connection.getTimeoutTime();

        if (getLogger(logger).isLoggable(Level.FINE)) {
            getLogger(logger).fine("returning socket to pool (" + address + "), timeout time=" + timeoutTime);
        }

        sslPool.put(address, connection, timeoutTime);
    }

    public ConnectionErrorAction returnErrorConnection(ServerConnection connection, Throwable exception, Logger logger) {
        getLogger(logger).log(Level.FINE, "error return", exception);

        ByteChannel channel = connection.channel();

        if (channel != null) {
            if (channel.isOpen()) {
                try {
                    channel.close();
                } catch (IOException e) {
                    // do nothing, don't care anymore
                }
            } else {
                getLogger(logger).warning("returned error connection is closed, retrying");

                return (ConnectionErrorAction.RETRY);
            }
        }

        getLogger(logger).fine("returning FAIL action");

        return ConnectionErrorAction.FAIL;
    }

    public void shutdown(Logger logger) {
        getLogger(logger).fine("shutting down socket pool provider");

        ServerConnection conn;

        while ((conn = sslPool.get(address)) != null) {
            SocketChannel channel = (SocketChannel)conn.channel();

            try {
                channel.close();
            } catch (IOException e) {
                // do nothing
            }
        }

        socketProvider.shutdown(logger);
    }

    // ---------------------------------------------------------------

    @Override
    public String toString() {
        // TODO: Add more SSL info here?
        return "SSLconn address=" + address.toString() + ", pool=" + sslPool.size(address) + "/"
                + socketProvider.getPoolSize();
    }

    // --------------------------------------------------------

    private Logger getLogger(Logger clientLogger) {
        return ((clientLogger == null) ? this.logger : clientLogger);
    }
    
    public void closeExpired(long currTime) {
        sslPool.closeExpired(currTime);
    }

    @Override
    public int getPort() {
        InetSocketAddress inetAddress = getAddress();
        return inetAddress == null ? 0 : inetAddress.getPort();
    }

    @Override
    public String getHostName() {
        InetSocketAddress inetAddress = getAddress();
        return inetAddress == null ? null : inetAddress.getHostName();
    }
}
