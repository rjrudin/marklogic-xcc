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
package com.marklogic.xcc.spi;

import java.io.IOException;
import java.util.logging.Logger;

import com.marklogic.xcc.Request;
import com.marklogic.xcc.Session;

/**
 * This interface defines a provider of Sockets for a {@link com.marklogic.xcc.ContentSource}.
 * Policies such as load balancing or failover can be implemented by supplying a custom
 * ConnectionProvider.
 */
public interface ConnectionProvider {
    /**
     * <p>
     * Provide a {@link ServerConnection}) that is open and ready to communicate with the server.
     * </p>
     * <p>
     * Note: The signature of this method changed in the 4.0 release to add the
     * {@link com.marklogic.xcc.Request} parameter. Code ported from earlier XCC releases will need
     * to be modified to add the new parameter.
     * </p>
     * 
     * @param session
     *            The {@link com.marklogic.xcc.Session} requesting a connection.
     * @param request
     *            The {@link com.marklogic.xcc.Request} that is about to be submitted on the
     *            connection.
     * @param logger
     *            A {@link java.util.logging.Logger} instance to use. @return An open, ready to use
     *            {@link ServerConnection} object.
     * @throws java.io.IOException
     *             If an underlying connection cannot be created.
     * @return An initialized {@link com.marklogic.xcc.spi.ServerConnection} instance.
     */
    ServerConnection obtainConnection(Session session, Request request, Logger logger) throws IOException;

    /**
     * Return a {@link ServerConnection} object (obtained from a previous call to
     * {@link #obtainConnection(com.marklogic.xcc.Session,com.marklogic.xcc.Request,java.util.logging.Logger)}
     * ) to the provider, possibly to be pooled and re-used. If the connection has a non-zero
     * timeout value set ({@link com.marklogic.xcc.spi.ServerConnection#setTimeoutMillis(long)}),
     * that value will inform the provider to discard the object after that amount of time has
     * elapsed.
     * 
     * @param connection
     *            A previously obtained {@link ServerConnection} instance.
     * @param logger
     *            A {@link Logger} instance to use.
     */
    void returnConnection(ServerConnection connection, Logger logger);

    /**
     * Return a connection that experienced an error. The provider will indicate the action the
     * client should take.
     * 
     * @param connection
     *            A previously obtained {@link ServerConnection} instance.
     * @param exception
     *            The (possibly null) exception that occurred. The provider may wish to decide which
     *            action to recommend depending on the type of exception.
     * @param logger
     *            A {@link Logger} instance to use.
     * @return An instance of {@link ConnectionErrorAction} instance that advises the client which
     *         action to take.
     */
    ConnectionErrorAction returnErrorConnection(ServerConnection connection, Throwable exception, Logger logger);

    /**
     * Tell the provider to shutdown and release any resources being used. It's possible this method
     * may never be called.
     * 
     * @param logger
     *            A {@link Logger} instance to use.
     */
    void shutdown(Logger logger);
    
    /**
     * Close expired resource in the provider pool.
     * 
     * @param currTime Current time to use for expired status check.
     */
    void closeExpired(long currTime);

    /**
     * Return the port of the connection provider
     * 
     * @return port number
     */
    int getPort();

    /**
     * Return the host name of the connection provider
     * 
     * @return the string of host name
     */
    String getHostName();
}
