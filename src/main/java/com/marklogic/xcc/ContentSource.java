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
package com.marklogic.xcc;

import java.util.logging.Logger;

import com.marklogic.xcc.spi.ConnectionProvider;

/**
 * <p>
 * A ContentSource object describes a content source (MarkLogic Server instance) and serves as a
 * factory that creates {@link Session} objects. Instances of ContentSource may be obtained from a
 * JNDI lookup service or from one of the static factory methods on the {@link ContentSourceFactory}
 * class.
 * </p>
 * <p>
 * See the {@link #getDefaultLogger()} method for information about configuring logging behavior.
 * </p>
 * 
 * @see #getDefaultLogger()
 * @see ContentSourceFactory
 * @see Session
 */
public interface ContentSource {
    /**
     * Attempts to establish a {@link Session} with the default contentbase for this ContentSource.
     * Login credentials are the defaults established when the instance was created or bound to the
     * JNDI service.
     * 
     * @return A {@link Session} instance.
     * @throws IllegalStateException
     *             If this ContentSource was created without specifying user/password credentials.
     */
    Session newSession();

    /**
     * Attempts to establish a {@link Session} with the specified contentbase on the server
     * represented by this ContentSource.
     * 
     * @param contentbaseId
     *            A contentbase name or numeric ID.
     * @return A {@link Session} instance.
     * @throws IllegalStateException
     *             If this ContentSource was created without specifying default user credentials.
     */
    Session newSession(String contentbaseId);

    /**
     * Attempts to establish a {@link Session} with the default contentbase for this ContentSource
     * using the provided login credentials.
     * 
     * @param userName
     *            The user name to connect as.
     * @param password
     *            The password associated with the user name.
     * @return A {@link Session} instance.
     */
    Session newSession(String userName, String password);

    /**
     * Attempts to establish a {@link Session} with the specified contentbase on the server
     * represented by this ContentSource, using the provided user credentials.
     * 
     * @param userName
     *            The user name to connect as.
     * @param password
     *            The password associated with the user name.
     * @param contentbaseId
     *            A contentbase name or numeric ID.
     * @return A {@link Session} instance.
     */
    Session newSession(String userName, String password, String contentbaseId);

    /**
     * <p>
     * Returns the current Logger to which log messages will be sent. If not overridden with
     * {@link #setDefaultLogger(Logger)}, an implementation default logger is returned. This Logger
     * will be inherited by {@link Session} instances created from this ContentSource. The
     * {@link Logger} for individual {@link Session}s can be overridden with the
     * {@link Session#setLogger(java.util.logging.Logger)} method.
     * </p>
     * <p>
     * The name of the implementation default logger is <code>com.marklogic.xcc</code>. This is the
     * name which should be used in a logging properties file (see
     * {@link java.util.logging.LogManager}) to customize the logger.
     * </p>
     * <p>
     * XCC includes a bundled properties file that augments the default JVM logging properties. This
     * file, xcc.logging.properties, is read from the classpath when the first ContentSource
     * instance is created. The properties file in xcc.jar <a
     * href="doc-files/xcc.logging.properties.txt">looks like this</a>. If you wish to customize
     * these logging properties, place a copy of this file in your classpath ahead of xcc.jar.
     * </p>
     * <p>
     * <a href="doc-files/xcc.logging.properties.txt">Default logging properties file in
     * xcc.jar</a>.
     * </p>
     * 
     * @return The currently set Logger instance, or a default created by the implementation.
     */
    Logger getDefaultLogger();

    // TODO: Provide an example/solution for using log4J loggers.
    /**
     * <p>
     * Set the default java.util.Logger instance which will be inherited by new {@link Session}
     * instances.
     * <p>
     * 
     * @param logger
     *            An instance of java.util.logging.Logger
     */
    void setDefaultLogger(Logger logger);
    
    /**
     * @return true if basic authentication will be attempted preemptively, false otherwise.
     */
    public boolean isAuthenticationPreemptive();
    
    /**
     * <p>Sets whether basic authentication should be attempted preemptively, default is false.</p>
     * 
     * <p>Preemptive authentication can reduce the overhead of making connections to servers that accept
     * basic authentication by eliminating the challenge-response interaction otherwise required.</p>  
     * 
     * <p>Note that misuse of preemptive authentication entails potential security risks, and under most 
     * circumstances the credentials used to authenticate will be cached after the first connection.  To
     * avoid creating the illusion that credentials are protected, connections to a server requiring digest 
     * authentication will not be retried if this flag is set.</p> 
     *
     * @param value true if basic authentication should be attempted preemptively, false otherwise.
     */
    public void setAuthenticationPreemptive(boolean value);

    /**
     * @return The ConnectionProvider used to construct this ContentSource.
     */
    public ConnectionProvider getConnectionProvider();
}
