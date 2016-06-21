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

import java.io.Closeable;
import java.math.BigInteger;
import java.net.URI;
import java.util.List;
import java.util.logging.Logger;
import javax.transaction.xa.XAResource;

import com.marklogic.xcc.exceptions.RequestException;

/**
 * <p>
 * A Session object represents a conversation with a contentbase (database) on a MarkLogic Server
 * instance ({@link ContentSource}) and holds state information related to that conversation.
 * Connections to the server are created and released as needed and are automatically pooled.
 * </p>
 * <p>
 * Sessions are created by invoking one of the {@link ContentSource#newSession()} factory methods.
 * </p>
 */
public interface Session extends Closeable {
    /**
     * The transaction mode for the session, which governs how transactions are
     * created, and whether they are automatically committed.
     */
    public enum TransactionMode {
        /**
         * Creates a new transaction for every request.  The type of transaction created 
         * is determined automatically by the query.
         */
        AUTO {
            public String toString() {
                return "auto";
            }
        },
        /**
         * Creates a read-only query transaction to group requests. The transaction must
         * be committed or rolled back explicitly.
         */
        QUERY {
            public String toString() {
                return "query";
            }
        },
        /**
         * Creates an updating transaction to group requests. The transaction must
         * be committed or rolled back explicitly.
         */
        UPDATE {
            public String toString() {
                return "update";
            }

            @Override
            public boolean isRetryable() {
                return false;
            }
        },
        /**          
         * Creates a new transaction for every request, and commits (or rolls back)          
         * the transaction at the end of that request.           
         */          
        UPDATE_AUTO_COMMIT {
            public String toString() {
                return "update-auto-commit";
            }
        };
        abstract public String toString();
        public boolean isRetryable() {
            return true;
        }
    }

    /**
     * Obtain a reference to the {@link ContentSource} object from which this Session instance was
     * obtained.
     * 
     * @return The {@link ContentSource} from which this session was instantiated.
     */
    ContentSource getContentSource();

    /**
     * Returns the user identity associated with this Session.
     * 
     * @return The user identity as a String.
     */
    UserCredentials getUserCredentials();

    /**
     * <p>
     * Return the contentbase name provided when this Session was created, if any. Note that this is
     * the name given, if any, when the Session and/or {@link ContentSource} was created. If no
     * explicit name was given then a default was used and this method will return null. To
     * determine the actual name of the contentbase associated with a Session, call
     * {@link com.marklogic.xcc.ContentbaseMetaData#getContentBaseName()}.
     * </p>
     * <p>
     * For example:
     * </p>
     * <p class="codesample">
     * String cbname = session.getContentbaseMetaData().getContentBaseName();
     * </p>
     * <p>
     * The above code makes a round-trip call to the server. This method is a convenience that
     * returns a locally stored {@link String}, or null.
     * </p>
     * 
     * @return The contentbase name stored in the Session, or null.
     * @see ContentSourceFactory#newContentSource(java.net.URI)
     * @see ContentSourceFactory#newContentSource(String, int, String, String, String)
     * @see ContentSource#newSession(String)
     * @see ContentSource#newSession(String, String, String)
     * @see ContentbaseMetaData
     * @see #getContentbaseMetaData()
     */
    String getContentBaseName();

	/**
	 * <p>
	 * Returns an instance of the XAResource interface, specific to this Session object.
	 * This can be used to take part in JTA distributed transactions using an implementation of
	 * javax.transaction.TransactionManager.
	 * </p>
	 *
     * @return The XAResource object.
	 */
	XAResource getXAResource();

    /**
     * <p>
     * Sets the transaction mode to the given value. The initial value is
     * TransactionMode.AUTO.
     * </p>
     * <p>
     * If the transaction mode is TransactionMode.AUTO, a new transaction is created for
     * every request.  The type of transaction created is determined automatically by the query.
     * </p>
     * <p>
     * If transaction mode is TransactionMode.QUERY or TransactionMode.UPDATE, requests
     * are grouped under transactions bounded by calls to Session.commit() or Session.rollback().
     * If transaction mode is TransactionMode.QUERY, then a read-only query transaction is created
     * to group requests. If transaction mode is TransactionMode.UPDATE, then a locking update
     * transaction is created. If an updating request is executed under a read-only
     * TransactionMode.QUERY transaction, a RequestException is thrown.
     * If transaction mode is TransactionMode.UPDATE_AUTO_COMMIT, a new transaction is created for
     * every request, and is committed (or rolled back) at the end of that request.
     * </p>
     * <p>
     * Calling setTransactionMode() while a transaction is active has no effect on the current
     * transaction.
     * </p>
     * @param mode The new transaction mode
     */
    void setTransactionMode(TransactionMode mode);

    /**
     * Get the current transaction mode.
     * 
     * @return The current transaction mode setting.
     */
    TransactionMode getTransactionMode();

    /**
     * Sets the timeout for transactions
     * @param seconds The number of seconds before the transaction times out
     * @throws RequestException
     *             If there is a problem communicating with the server.
     */
    void setTransactionTimeout(int seconds) throws RequestException;

    /**
     * Get the current transaction timeout by querying the server.
     *
     * @return The current transaction timeout setting.
     * @throws RequestException
     *             If there is a problem communicating with the server.
     */
    int getTransactionTimeout() throws RequestException;
    
    /**
     * Get the current transaction timeout cached in XCC.
     *
     * @return The current transaction timeout setting.
     */
    int getCachedTxnTimeout();

    /**
     * Commits the current transaction.
     * @throws IllegalStateException
     *             If transaction mode is set to TransactionMode.AUTO.
     * @throws RequestException
     *             If there is a problem communicating with the server.
     */
    void commit() throws RequestException;

    /**
     * Rolls back the current transaction.
     * @throws IllegalStateException
     *             If transaction mode is set to TransactionMode.AUTO.
     * @throws RequestException
     *             If there is a problem communicating with the server.
     */
    void rollback() throws RequestException;

    /**
     * Shutdown and invalidate this Session and release any resources it may be holding. Any
     * currently open {@link ResultSequence} objects created from this Session will be
     * invalidated and closed, and any open transaction will be rolled back.
     */
    void close();

    /**
     * True if this Session object is closed.
     * 
     * @return true if this Session has been closed.
     */
    boolean isClosed();

    /**
     * Submit a {@link Request} to the contentbase and return the (possibly empty)
     * {@link ResultSequence}.
     * 
     * @param request
     *            A {@link Request} instance (either {@link ModuleInvoke} or {@link AdhocQuery} that
     *            specifies the query to be run, associated options and variables.
     * @return A {@link ResultSequence} instance encapsulating the result of the query execution.
     * @throws IllegalStateException
     *             If this Session has been closed.
     * @throws RequestException
     *             If there is a problem communicating with the server.
     */
    ResultSequence submitRequest(Request request) throws RequestException;

    /**
     * Create a new {@link AdhocQuery} object and initialize it with the given query string and
     * {@link RequestOptions} object.
     * 
     * @param queryText
     *            The ad-hoc XQuery code to be evaluated.
     * @param options
     *            An instance of {@link RequestOptions} to be set on the return {@link AdhocQuery}
     *            object. This can be overridden later.
     * @return An initialized instance of {@link AdhocQuery}.
     */
    AdhocQuery newAdhocQuery(String queryText, RequestOptions options);

    /**
     * Create a new {@link AdhocQuery} object and initialize it with the given query string.
     * 
     * @param queryText
     *            The ad-hoc XQuery code to be evaluated.
     * @return An initialized instance of {@link AdhocQuery}.
     */
    AdhocQuery newAdhocQuery(String queryText);

    /**
     * Create a new {@link ModuleInvoke} object and initialize it with the given module URI and
     * {@link RequestOptions} object.
     * 
     * @param moduleUri
     *            The URI of a module on the server to be invoked.
     * @param options
     *            An instance of {@link RequestOptions} to be set on the returned
     *            {@link ModuleInvoke} object. This can be overridden later.
     * @return An initialized instance of {@link ModuleInvoke}.
     * @see ModuleInvoke
     */
    ModuleInvoke newModuleInvoke(String moduleUri, RequestOptions options);

    /**
     * Create a new {@link ModuleInvoke} object and initialize it with the given module URI.
     * 
     * @param moduleUri
     *            The URI of a module on the server to be invoked.
     * @return An initialized instance of {@link ModuleInvoke}.
     * @see ModuleInvoke
     */
    ModuleInvoke newModuleInvoke(String moduleUri);

    /**
     * Create a new {@link ModuleSpawn} object and initialize it with the given module URI and
     * {@link RequestOptions} object.
     * 
     * @param moduleUri
     *            The URI of a module on the server to be invoked.
     * @param options
     *            An instance of {@link com.marklogic.xcc.RequestOptions} to be set on the returned
     *            {@link com.marklogic.xcc.ModuleSpawn} object. This can be overridden later.
     * @return An initialized instance of {@link ModuleSpawn}.
     * @see ModuleInvoke
     */
    ModuleSpawn newModuleSpawn(String moduleUri, RequestOptions options);

    /**
     * Create a new {@link ModuleSpawn} object and initialize it with the given module URI.
     * 
     * @param moduleUri
     *            The URI of a module on the server to be invoked.
     * @return An initialized instance of {@link ModuleSpawn}.
     * @see ModuleInvoke
     */
    ModuleSpawn newModuleSpawn(String moduleUri);

    /**
     * <p>
     * Insert the given {@link Content} into the contentbase. This is equivalent to calling
     * {@link #insertContent(Content[])} with an array length of one. Upon return, the content will
     * have been inserted and committed.
     * </p>
     * <p>
     * {@link Content} objects that are rewindable will be automatically retried if a recoverable
     * error occurs during transmission to the server. To specify the maximum number of retry
     * attemtps, set an instance of {@link RequestOptions} with the desired value (
     * {@link RequestOptions#setMaxAutoRetry(int)}) using the
     * {@link #setDefaultRequestOptions(RequestOptions)} method prior to invoking this method.
     * </p>
     * <p>
     * The retry/timeout algorithm is different for document insert than for query requests. For
     * queries, a constant delay ocurrs between each retry. For inserts, an exponentially increasing
     * backoff delay is used. Retryable exceptions usually ocurr during document insert when a
     * cluster communication recover is in progress. Inter-try delays increase up to a maximum of
     * about two seconds until the retry count is exhausted. The default first delay is 125
     * milliseconds with a count of 64, which works out to an overall retry interval of about two
     * minutes before giving up.
     * </p>
     * 
     * @param content
     *            A single {@link Content} instance to be inserted in the contentbase.
     * @throws IllegalStateException
     *             If this Session has been closed.
     * @throws RequestException
     *             If there is a problem communicating with the server.
     */
    void insertContent(Content content) throws RequestException;

    /**
     * <p>
     * Insert all the {@link Content} objects in the contentbase as a transactional unit. If this 
     * is called within an auto-commit transaction, all documents will have been committed upon
     * successful return. If an exception is thrown, none of the documents will have been committed.
     * </p>
     * <p>
     * The presence in the array of multiple {@link Content} objects with the same URI is not
     * considered an error.  Though all of the documents are committed together, each document 
     * insertion is processed separately, in array order, and the last write wins.
     * </p>
     * 
     * @param content
     *            An array of {@link Content} objects that are inserted as a group atomically.
     * @throws IllegalStateException
     *             If this Session has been closed.
     * @throws RequestException
     *             If there is a problem communicating with the server.
     */
    void insertContent(Content[] content) throws RequestException;

    /**
     * <p>
     * Insert all the {@link Content} objects in the contentbase. If a {@link RequestServerException}
     * is caught the remaining documents will continue to be submitted. 
     * </p>
     * <p>
     * If the session's TransactionMode is UPDATE, the transaction contains all
     * successful inserts, and all failed inserts are rolled back automatically.
     * </p>
     * <p>
     * If the session's TransactionMode is AUTO, all successful inserts will be
     * committed upon return except if the last document in the array caused an
     * error.  In that case, the entire transaction will be rolled back.
     * </p>
     * The presence in the array of multiple {@link Content} objects with the 
     * same URI is not considered an error.  Though all of the documents are 
     * committed together, each document insertion is processed separately, in 
     * array order, and the last write wins.
     * </p>
     * 
     * @param content
     *            An array of {@link Content} objects that are inserted as a 
     *            group in the current transaction.
     * @return null if no error is caught; otherwise, a list of 
     *            RequestException.
     * @throws IllegalStateException
     *            If this Session has been closed or if the session is not in
     *            UPDATE transaction mode.
     * @throws RequestException
     *             If there is a problem communicating with the server.
     */
    List<RequestException> insertContentCollectErrors(Content[] content) 
    throws RequestException;

    /**
     * Meta-information about the contentbase associated with this Session.
     * 
     * @return An instance of {@link ContentbaseMetaData}.
     */
    ContentbaseMetaData getContentbaseMetaData();

    /**
     * This method accepts an instance of {@link RequestOptions} which acts as the default settings
     * for invocations of {@link #submitRequest(Request)}. These defaults may be overridden by a
     * {@link RequestOptions} instance on individual requests. If a {@link RequestOptions} object is
     * set on both the Session and the {@link Request}, then both are applied with the values of the
     * {@link Request} object taking precedence.
     * 
     * @param options
     *            An instance of {@link RequestOptions}. A value of null indicates that defaults
     *            should be re-applied.
     */
    void setDefaultRequestOptions(RequestOptions options);

    /**
     * Returns the {@link RequestOptions} instance set on this Session object. An instance of
     * {@link RequestOptions} with default settings is created when the {@link Session} is created
     * and whenever you pass null to {@link #setDefaultRequestOptions(RequestOptions)}, so this
     * method always returns a non-null value.
     * 
     * @return An instance of {@link RequestOptions}.
     */
    RequestOptions getDefaultRequestOptions();

    /**
     * Returns an instance of {@link RequestOptions} that represents the effective default request
     * options for this Session (ie, the options that would be applied if no options are applied to
     * a specific {@link Request}. The values in the returned instance reflect the builtin defaults
     * merged with the values in the options set by
     * {@link #setDefaultRequestOptions(RequestOptions)} (if any). The object returned is a copy,
     * making changes to it will not affect option settings for the Session.
     * 
     * @return An instance of {@link RequestOptions}.
     */
    RequestOptions getEffectiveRequestOptions();

    /**
     * Issues a query to the server and returns the most recent system commit timestamp. The value
     * returned is as-of the time the query executes on the server. The returned value represents a
     * point-in-time, or snapshot, of the contentbase. This value may be passed to
     * {@link RequestOptions#setEffectivePointInTime(java.math.BigInteger)} to run queries as-of
     * that contentbase state.
     * 
     * @return A {@link java.math.BigInteger} value containing a point-in-time timestamp.
     * @throws RequestException
     *             If there is a problem communicating with the server.
     */
    BigInteger getCurrentServerPointInTime() throws RequestException;

    /**
     * Get the java.util.logging.Logger instance set on this Session. If not explictly set with
     * {@link #setLogger(Logger)}, the Logger inherited from the creating {@link ContentSource} is
     * returned.
     * 
     * @return A Logger instance
     * @see com.marklogic.xcc.ContentSource#getDefaultLogger()
     * @see com.marklogic.xcc.ContentSource#setDefaultLogger(Logger)
     */
    Logger getLogger();

    /**
     * Set the Logger instance to which log messages resulting from operations on this Session
     * should be written.
     * 
     * @param logger
     *            An instance of java.util.Logger
     */
    void setLogger(Logger logger);

    /**
     * Attach, or clear, an opaque user-provided object to this Session. This object is not used or
     * examined in any way by XCC. Client code may use this hook in any way it sees fit to associate
     * state with the Session.
     * 
     * @param userObject
     *            An opaque {@link Object} that may later be retrieved with {@link #getUserObject()}
     *            . A value of null is acceptable.
     */
    void setUserObject(Object userObject);

    /**
     * Fetch the current user object set for this Session.
     * 
     * @return Return the object reference previously passed to {@link #setUserObject(Object)}, or
     *         null.
     */
    Object getUserObject();

    /**
     * <p>
     * Return a URI that describes connection information for this Session, if available. Connection
     * information is dependent on the {@link com.marklogic.xcc.spi.ConnectionProvider} encapsulated
     * in the {@link com.marklogic.xcc.ContentSource} that created this Session. In most cases, the
     * provider will be the built-in socket-based provider included with XCC. If a custom provider
     * is in use and does not implement point-to-point socket connections, then this method will
     * return null.
     * </p>
     * <p>
     * Otherwise, a {@link java.net.URI} instance is returned that contains information describing
     * the connections that will be made to the server. For security reasons, the password provided
     * when the {@link com.marklogic.xcc.ContentSource} or Session was created is not returned. The
     * password will be masked as "xxxx". The returned {@link java.net.URI} may be used to
     * instantiate a new {@link com.marklogic.xcc.ContentSource}, but the real password will need to
     * be provided when calling {@link com.marklogic.xcc.ContentSource#newSession(String, String)}.
     * </p>
     * 
     * @return An instance of {@link java.net.URI}, or null if the underlying
     *         {@link com.marklogic.xcc.spi.ConnectionProvider} does not implement convetional
     *         point-to-point socket connections, or if a {@link java.net.URI} object cannot be
     *         created from this Session.
     * @since 3.2
     * @see ContentSourceFactory#newContentSource(java.net.URI)
     */
    URI getConnectionUri();
}
