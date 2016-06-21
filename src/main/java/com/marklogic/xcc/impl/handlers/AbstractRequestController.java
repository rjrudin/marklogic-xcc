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
package com.marklogic.xcc.impl.handlers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.marklogic.http.HttpChannel;
import com.marklogic.xcc.Request;
import com.marklogic.xcc.RequestOptions;
import com.marklogic.xcc.ResultSequence;
import com.marklogic.xcc.Session;
import com.marklogic.xcc.exceptions.RequestException;
import com.marklogic.xcc.exceptions.RequestPermissionException;
import com.marklogic.xcc.exceptions.RequestServerException;
import com.marklogic.xcc.exceptions.RetryableQueryException;
import com.marklogic.xcc.exceptions.ServerConnectionException;
import com.marklogic.xcc.exceptions.ServerResponseException;
import com.marklogic.xcc.exceptions.UnexpectedResponseException;
import com.marklogic.xcc.impl.ContentSourceImpl;
import com.marklogic.xcc.impl.SessionImpl;
import com.marklogic.xcc.spi.ConnectionErrorAction;
import com.marklogic.xcc.spi.ConnectionProvider;
import com.marklogic.xcc.spi.ServerConnection;

public abstract class AbstractRequestController implements HttpRequestController {
    protected final Map<Integer, ResponseHandler> handlers;
    protected static final Integer DEFAULT_HANDLER_KEY = new Integer(0);

    protected AbstractRequestController(Map<Integer, ResponseHandler> handlers) {
        if (handlers == null) {
            this.handlers = new HashMap<Integer, ResponseHandler>();
        } else {
            this.handlers = handlers;
        }
    }

    // -------------------------------------------------------------

    public abstract ResultSequence serverDialog(ServerConnection connection, Request request,
            RequestOptions effectiveOptions, Logger logger) 
    throws IOException, RequestException;

    // -------------------------------------------------------------
    // HttpRequestController interface

    public ResultSequence runRequest(ConnectionProvider provider, Request request, Logger logger)
            throws RequestException {
        SessionImpl session = (SessionImpl)request.getSession();
        RequestOptions options = request.getEffectiveOptions();
        long delayMillis = options.getAutoRetryDelayMillis();
        int retries = options.getMaxAutoRetry();
        int tries = Math.max(retries + 1, 1);
        RequestException re = null;

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("submitting request, max tries=" + tries);
        }
        // Retry logic shouldn't run for multi-request transactions
        int t = 0;
        for (; t < tries; t++) {
            ServerConnection connection = null;

            sleepFor(interTryDelay(delayMillis, t));

            try {
                while (true) {

                    connection = provider.obtainConnection(session, request, logger);

                    try {
                        ResultSequence rs = serverDialog(connection, request, options, logger);

                        if ((rs == null) || rs.isCached()) {
                            provider.returnConnection(connection, logger);
                        }

                        return rs;
                    } catch (RequestPermissionException e) {
                        if (e.isRetryAdvised()) {
                            // avoid unnecessary message construction for this common exception
                            if (logger.isLoggable(Level.FINE)) {
                                logger.log(Level.FINE, "Retryable permission exception caught.", e);
                            }
                            provider.returnConnection(connection, logger);
                        } else {
                            provider.returnConnection(connection, logger);
                            throw e;
                        }
                    }
                }
            } catch (RetryableQueryException e) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE,
                            "Retryable server exception caught.", e);
                }
                provider.returnConnection(connection, logger);
                re = e;
            } catch (ServerResponseException e) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE,
                            "ServerResponseException caught.", e);
                }
                provider.returnConnection(connection, logger);
                throw e;
            } catch (ServerConnectionException e) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE,
                            "Retryable server exception caught.", e);
                }
                provider.returnConnection(connection, logger);
                re = e;
            } catch (RequestServerException e) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, 
                            "Non-retryable server exception caught.", e);
                }
                provider.returnConnection(connection, logger);
                throw e;
            
            } catch (IOException e) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "Connection IOException caught.", 
                            e);
                }
              
                ConnectionErrorAction action = null;

                if (connection != null) {
                    action = provider.returnErrorConnection(connection, e, logger);
                }

                re = new ServerConnectionException(e.getMessage(), request, e);
                 
                boolean badResponse = e.getCause() instanceof 
                        UnexpectedResponseException;
                if (badResponse) {
                    logger.log(Level.WARNING, e.getMessage());
                }
                if (action != ConnectionErrorAction.RETRY && !badResponse) {
                    if (action == null) {
                        logger.log(Level.WARNING, "Cannot obtain connection: " + 
                            e.getMessage(), e);
                    } else if (logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE, "Provider error action=" + 
                            action + ", throwing: " + re, re);
                    }

                    throw re;
                }
            }

            if (session.getTxnID() == null && session.getTransactionMode() != null &&
               !session.getTransactionMode().isRetryable()) {
                if(re != null && 
                   !(re.getCause() instanceof UnexpectedResponseException)) 
                    throw re;
                break;
            }
        }
        if (logger.isLoggable(Level.INFO)) {
            logger.log(Level.INFO, "automatic query retries (" + t + 
                    ") exhausted, throwing: " + re, re);
        }

        if (re != null) {
            throw re;
        }

        String msg = "BAD INTERNAL STATE: retries exhausted, no prior retryable exception";
        logger.severe(msg);

        throw new RequestException(msg, request);
    }

    // -------------------------------------------------------------
    // subclass accessor methods

    protected void addHandler(int code, ResponseHandler handler) {
        addHandler(handlers, code, handler);
    }

    protected static void addHandler(Map<Integer, ResponseHandler> handlers, int code, ResponseHandler handler) {
        handlers.put(new Integer(code), handler);
    }

    protected void addDefaultHandler(ResponseHandler handler) {
        addDefaultHandler(handlers, handler);
    }

    protected static void addDefaultHandler(Map<Integer, ResponseHandler> handlers, ResponseHandler handler) {
        handlers.put(DEFAULT_HANDLER_KEY, handler);
    }

    protected ResponseHandler findHandler(int responseCode) {
        ResponseHandler handler = handlers.get(new Integer(responseCode));

        if (handler != null) {
            return handler;
        }

        return handlers.get(DEFAULT_HANDLER_KEY);
    }

    // -------------------------------------------------------------

    public static boolean dontSleep = false; // this is a unit test hook

    protected abstract long interTryDelay(long delay, int currentTry);

    private void sleepFor(long millis) {
        if (dontSleep || (millis <= 0))
            return;

        long wakeupTime = System.currentTimeMillis() + millis;
        long now;

        while ((now = System.currentTimeMillis()) < wakeupTime) {
            long sleepTime = wakeupTime - now;

            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                // nothing, go around
            }
        }
    }

    protected void setConnectionTimeout(ServerConnection connection, HttpChannel http) {
        long expiryTime = 0;

        try {
            expiryTime = http.getResponseKeepaliveExpireTime();
        } catch (IOException e) {
            // do nothing, default to 0
        }

        connection.setTimeoutTime(expiryTime);
    }

    protected void addCommonHeaders(HttpChannel http, SessionImpl session, String method, String uri,
            RequestOptions options, Logger logger) {
    	
        ContentSourceImpl contentSource = (ContentSourceImpl) session.getContentSource();

        String authorization = contentSource.getAuthString(method, uri, session.getUserCredentials());

        if (authorization != null) {
            http.setRequestHeader("Authorization", authorization);
        }

        http.setRequestHeader("User-Agent", session.userAgentString());
        http.setRequestHeader("Accept", session.getAcceptedContentTypes());
        
        if (HttpChannel.isUseHTTP()) {
            ConnectionProvider cp = contentSource.getConnectionProvider();
            http.setRequestHeader("Host", cp.getHostName() + ":" + cp.getPort());
        }

        if (options.getRequestName() != null) {
            http.setRequestHeader("Referer", options.getRequestName());
        }

        // Session cookie
        if(session.getSessionID() != null) {
            http.setRequestHeader("Cookie", "SessionID=" + session.getSessionID());
            logger.fine("Sending SessionID: " + session.getSessionID() + ", TxnID: "
            + session.getTxnID() + ", TxnMode: " + session.getTransactionMode());
        }
    }
}
