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

import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.IOException;
import javax.transaction.xa.XAResource;

import com.marklogic.xcc.AdhocQuery;
import com.marklogic.xcc.Content;
import com.marklogic.xcc.ContentSource;
import com.marklogic.xcc.ContentbaseMetaData;
import com.marklogic.xcc.ModuleInvoke;
import com.marklogic.xcc.ModuleSpawn;
import com.marklogic.xcc.Request;
import com.marklogic.xcc.RequestOptions;
import com.marklogic.xcc.ResultItem;
import com.marklogic.xcc.ResultSequence;
import com.marklogic.xcc.Session;
import com.marklogic.xcc.UserCredentials;
import com.marklogic.xcc.Version;
import com.marklogic.xcc.exceptions.RequestException;
import com.marklogic.xcc.exceptions.StreamingResultException;
import com.marklogic.xcc.exceptions.XQueryException;
import com.marklogic.xcc.impl.handlers.ContentInsertController;
import com.marklogic.xcc.impl.handlers.EvalRequestController;
import com.marklogic.xcc.spi.ConnectionProvider;
import com.marklogic.xcc.spi.SingleHostAddress;
import com.marklogic.xcc.types.XSDecimal;
import com.marklogic.xcc.types.XSInteger;
import com.marklogic.http.HttpChannel;

public class SessionImpl implements Session {

    private final Set<StreamingResultSequence> activeResultSeqs;
    private final ContentSource contentSource;
    private final ConnectionProvider provider;
    private final UserCredentials credentials;
    private final String contentBase;
    private XAResourceImpl xaResource = null;
    private Logger logger = null;
    private RequestOptions defaultOptions = new RequestOptions();
    String sessionID = null;
    String txnID = null;
    private TransactionMode txnMode;
	private int timeout = 0;
    private boolean closed = false;
    boolean inXATxn = false;
    private Object userObject = null;
    private String serverVersion = null;
    private Throwable created = new Throwable();
    private boolean compatibleTxnMode = 
            System.getProperty("xcc.txn.compatible", "false").equals("true");
    private boolean txnModeChanged = false; 
    
    private final static boolean envCompactSequencesEnabled = 
            System.getProperty("xcc.compact.sequences", "true").equals("true");
    
    private boolean compactSequencesEnabled = envCompactSequencesEnabled;
    
	private static final String agentString = "Java/" + System.getProperty("java.version") + " MarkLogicXCC/"
            + Version.getVersionMajor() + "." + Version.getVersionMinor() + "-" + Version.getVersionPatch();

    public boolean isCompactSequencesEnabled() {
        return compactSequencesEnabled;
    }

    public void setCompactSequencesEnabled(boolean compactSequencesEnabled) {
        this.compactSequencesEnabled = compactSequencesEnabled;
    }

    public SessionImpl(ContentSource contentSource, ConnectionProvider connectionProvider, UserCredentials credentials,
            String contentBase) {
        this.contentSource = contentSource;
        this.provider = connectionProvider;
        this.credentials = credentials;
        this.contentBase = contentBase;

        activeResultSeqs = Collections.synchronizedSet(new HashSet<StreamingResultSequence>());
    }

    public SessionImpl clone()
    {
        return new SessionImpl(contentSource,provider,credentials,contentBase);
    }

    public void setServerVersion(String serverVersion) {
    	this.serverVersion = serverVersion;
    }
    
    public String getServerVersion() {
    	return serverVersion;
    }
    
    // -------------------------------------------------------------
    // Session interface

    public UserCredentials getUserCredentials() {
        return credentials;
    }

    public String getContentBaseName() {
        return contentBase;
    }

    public ContentSource getContentSource() {
        return (contentSource);
    }

    public XAResource getXAResource() {
        if(xaResource == null)
            xaResource = new XAResourceImpl(this);
        return xaResource;
    }

    public void setTransactionMode(TransactionMode mode) {
        if(getTxnID() != null)
            throwIllegalState("Cannot call setTransactionMode() when there is an active transaction");
        txnMode = mode;
        txnModeChanged = true;
    }

    public TransactionMode getTransactionMode() {
        return txnMode;
    }

    public void setTransactionTimeout(int seconds) throws RequestException {
        if(getTxnID() != null) {
            submitRequestInternal(new AdhocImpl(this,
                    "xquery version '1.0-ml';\n" +
                    "xdmp:set-transaction-time-limit(" + seconds + ")", null)).close();
        }
        timeout = seconds;
    }

    public int getTransactionTimeout() throws RequestException {
        if(getTxnID() != null) {
            ResultSequence rs = submitRequestInternal(new AdhocImpl(this,
                     "xquery version '1.0-ml';\n" +
                    "xdmp:host-status(xdmp:host())//*:transaction[*:transaction-id eq xdmp:transaction()]" +
                    "/*:time-limit/string()", null));
            try {
                timeout = Integer.parseInt(rs.next().asString());
            } finally {
                rs.close();
            }
        }
        return timeout;
    }
    
    public int getCachedTransactionTimeout() {
        return timeout;
    }

    public void commit() throws RequestException {
        assertSessionOpen();
        if(getTxnID() == null)
            throwIllegalState("Cannot commit without an active transaction");
        submitRequestInternal(new AdhocImpl(this, "xquery version '1.0-ml'; xdmp:commit()", null)).close();
    }

    public void rollback() throws RequestException {
        assertSessionOpen();
        if(getTxnID() != null)
            submitRequestInternal(new AdhocImpl(this, "xquery version '1.0-ml'; xdmp:rollback()", null)).close();
    }

    public void close() {
        if (closed) return;

        boolean doRollback = false;
        synchronized(this) {
            doRollback = !inXATxn && txnID != null;
        }

        if(doRollback) {
            try {
                rollback();
            } catch(XQueryException e) {
                // Ignore XDMP-NOTXN
                if(!e.equals("XDMP-NOTXN"))
                    getLogger().log(Level.WARNING, "Exception rolling back during Session.close()", e);
            } catch(Exception e) {
                getLogger().log(Level.WARNING, "Exception rolling back during Session.close()", e);
            }
        }

        closed = true;

        synchronized (activeResultSeqs) {
            for (Iterator<StreamingResultSequence> it = activeResultSeqs.iterator(); it.hasNext();) {
                StreamingResultSequence rs = it.next();

                it.remove();

                try {
                    rs.close();
                } catch (StreamingResultException e) {
                    getLogger().log(Level.WARNING, "Exception closing streaming result sequence.", e);
                    // carry on
                }
            }
        }
    }

    public boolean isClosed() {
        return closed;
    }

    @Override
    public void finalize() {
        try {
            if(!closed) {
                if(getTxnID() != null)
                    getLogger().log(Level.SEVERE, 
                            "Destructing Session object with open transaction " + 
                            getTxnID() + ": ", created);
                close();
            }
        } catch(Throwable t) {
            getLogger().log(Level.SEVERE, "Exception during SessionImpl.finalize()", t);
        }
    }

    public ModuleInvoke newModuleInvoke(String moduleUri, RequestOptions options) {
        assertSessionOpen();

        return new ModuleImpl(this, moduleUri, options, false);
    }

    public ModuleInvoke newModuleInvoke(String moduleUri) {
        return (newModuleInvoke(moduleUri, null));
    }

    public ModuleSpawn newModuleSpawn(String moduleUri, RequestOptions options) {
        assertSessionOpen();

        return new ModuleImpl(this, moduleUri, options, true);
    }

    public ModuleSpawn newModuleSpawn(String moduleUri) {
        return (newModuleSpawn(moduleUri, null));
    }

    public AdhocQuery newAdhocQuery(String queryText, RequestOptions options) {
        assertSessionOpen();

        return new AdhocImpl(this, queryText, options);
    }

    public AdhocQuery newAdhocQuery(String queryText) {
        assertSessionOpen();

        return (newAdhocQuery(queryText, null));
    }

    public void insertContent(Content[] contents) throws RequestException {
        insertContent(contents, false);
    }
    
    public List<RequestException> insertContentCollectErrors(Content[] contents) 
    throws RequestException {
        return insertContent(contents, true);
    }
    
    public List<RequestException> insertContent(Content[] contents, 
            boolean collectErrors) throws RequestException {
        assertSessionOpen();

        Request request = newAdhocQuery("()");
        RequestOptions sessionOptions = getDefaultRequestOptions();

        // These numbers correspond to the server values, about two minutes overall
        if ((sessionOptions.getMaxAutoRetry() == -1) || (sessionOptions.getAutoRetryDelayMillis() == -1)) {
            RequestOptions options = new RequestOptions();
            if (sessionOptions.getMaxAutoRetry() == -1)
                options.setMaxAutoRetry(64);
            if (sessionOptions.getAutoRetryDelayMillis() == -1)
                options.setAutoRetryDelayMillis(125);
            request.setOptions(options);
        }

        assertNoTimeStamp(request);
        assertNonEmptyUris(request, contents);

        if (compatibleTxnMode) {
            createTransaction(request);
        }
        ContentInsertController controller = 
            new ContentInsertController(contents, txnMode, collectErrors);
        controller.runRequest(provider, request, getLogger());
        return controller.getErrors();
    }
    
    public boolean isInCompatibleMode() {
        return compatibleTxnMode;
    }
    
    public void setCompatibleMode(boolean mode) {
        compatibleTxnMode = mode;
    }

    private void createTransaction(Request request) throws RequestException {
        if((getTxnID() == null && txnMode != null && 
            txnMode != TransactionMode.AUTO) || txnModeChanged) {
            // Set the new transaction mode on the server, creating a new
            // current transaction if one does not already exist
            RequestOptions options = new RequestOptions();
            RequestOptions reqOpt = request.getOptions();
            options.setAutoRetryDelayMillis(reqOpt.getAutoRetryDelayMillis());
            options.setMaxAutoRetry(reqOpt.getMaxAutoRetry());
            options.setRequestName(reqOpt.getRequestName());
            options.setRequestTimeLimit(reqOpt.getRequestTimeLimit());
            options.setTimeoutMillis(reqOpt.getTimeoutMillis());
            options.setTimeZone(reqOpt.getTimeZone());
                
            submitRequestInternal(
                    new AdhocImpl(this, "xquery version '1.0-ml';\n"
                            + "declare option xdmp:transaction-mode '"
                            + serializeTransactionMode(txnMode)
                            + "'; "
                            + (timeout == 0 || getTxnID() != null ? "()"
                                    : "xdmp:set-transaction-time-limit("
                                            + timeout + ")"), options)).close();
        }
        
    }
    
    private String serializeTransactionMode(TransactionMode mode) {
        switch(mode) {
        case AUTO: return "auto";
        case QUERY: return "query";
        case UPDATE: return "update";
        case UPDATE_AUTO_COMMIT: 
        throwIllegalArg(
            "Transaction mode UPDATE_AUTO_COMMIT is not supported when " +
            "xcc.txn.compatible is set to true", 
            getLogger());
        }
        throwIllegalArg(
            "Unknown transaction mode: should be TransactionMode.AUTO, " +
            "TransactionMode.QUERY, TransactionMode.UPDATE", getLogger());
        return null;
    }

    private void assertNonEmptyUris(Request request, Content[] contents) throws RequestException {
        for (int i = 0; i < contents.length; i++) {
            String uri = contents[i].getUri();

            if ((uri == null) || (uri.length() == 0)) {
                throw new RequestException("Content insertion with empty URI is not allowed", request);
            }
        }
    }

    private void assertNoTimeStamp(Request request) throws RequestException {
        RequestOptions options = getEffectiveRequestOptions();

        if (options.getEffectivePointInTime() == null) {
            return;
        }

        if (options.getEffectivePointInTime().equals(BigInteger.ZERO)) {
            return;
        }

        throw new RequestException("Content insertion not allowed with non-zero Point-In-Time", request);
    }

    public void insertContent(Content content) throws RequestException {
        insertContent(new Content[] { content });
    }

    public ContentbaseMetaData getContentbaseMetaData() {
        return new CBMetaDataImpl(this);
    }

    public void setDefaultRequestOptions(RequestOptions options) {
        if (options == null) {
            this.defaultOptions = new RequestOptions();
        } else {
            this.defaultOptions = options;
        }
    }

    public RequestOptions getDefaultRequestOptions() {
        return defaultOptions;
    }

    public Logger getLogger() {
        return (logger == null) ? contentSource.getDefaultLogger() : logger;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public ResultSequence submitRequest(Request request) throws RequestException {
        assertSessionOpen();

        if ((request.getSession() != this) || (!(request instanceof RequestImpl))) {
            throwIllegalArg("Request object was not created by this session", getLogger());
        }
        
        if (compatibleTxnMode) {
            createTransaction(request);
        }

        return submitRequestInternal((RequestImpl)request);
    }

    ResultSequence submitRequestInternal(RequestImpl req) throws RequestException {
        EvalRequestController controller =  new EvalRequestController(req.serverPath(), req.encodedQueryString(getLogger())); 
        return controller.runRequest(provider, req, getLogger());
    }

    public BigInteger getCurrentServerPointInTime() throws RequestException {
        Request pitReq = newAdhocQuery("xdmp:request-timestamp()");

        ResultSequence rs = submitRequest(pitReq);
        ResultItem item = rs.next();
        BigInteger stamp = null;

        if (item.getItem() instanceof XSDecimal) {
            stamp = ((XSDecimal)item.getItem()).asBigDecimal().toBigInteger();
        } else {
            stamp = ((XSInteger)item.getItem()).asBigInteger();
        }

        rs.close();

        return stamp;
    }

    public Object getUserObject() {
        return userObject;
    }

    public void setUserObject(Object userObject) {
        this.userObject = userObject;
    }

    public URI getConnectionUri() {
        if (!(provider instanceof SingleHostAddress)) {
            return null;
        }

        InetSocketAddress addr = ((SingleHostAddress)provider).getAddress();

        try {
            return new URI("xcc", getUserCredentials().getUserName() + ":xxxx", addr.getHostName(), addr.getPort(),
                    (getContentBaseName() == null) ? null : ("/" + getContentBaseName()), null, null);
        } catch (URISyntaxException e) {
            return null;
        }
    }

    // ---------------------------------------------------------------
    // Implementation-specific accessors

    public void registerResultSequence(StreamingResultSequence resultSequence) {
        activeResultSeqs.add(resultSequence);
    }

    public void deRegisterResultSequence(StreamingResultSequence resultSequence) {
        synchronized (activeResultSeqs) {
            if (activeResultSeqs.contains(resultSequence)) {
                activeResultSeqs.remove(resultSequence);
            }
        }
    }

    public RequestOptions getEffectiveRequestOptions() {
        RequestOptions eff = new RequestOptions();
        RequestOptions ses = getDefaultRequestOptions();

        eff.applyEffectiveValues(new RequestOptions[] { ses });

        return eff;
    }

    public ConnectionProvider getProvider() {
        return provider;
    }

    public String userAgentString() {
        return (agentString);
    }

    public synchronized String getSessionID() {
        return sessionID;
    }

    public synchronized String getTxnID() {
        return txnID;
    }
    
    public String getAcceptedContentTypes() {
        if (compactSequencesEnabled) { 
            return "application/vnd.marklogic.sequence, */*";
        } else {
            return "*/*";
        }
    }

    public boolean readCookieValues(HttpChannel http) throws IOException {
        synchronized(this) {
            String session = http.getReponseCookieValue("SessionID");
            if(session != null) sessionID = session;

            String txn = http.getReponseCookieValue("TxnID");
            if(txn != null) txnID = txn.equals("null") ? null : txn;
        }

        String mode = http.getReponseCookieValue("TxnMode");
        TransactionMode newMode = null;
        if (mode != null) {
            newMode = parseTransactionMode(mode);
        }
        if (txnMode != null && txnMode != TransactionMode.AUTO &&
            !isTxnCompatible()) {
            compatibleTxnMode = true;
            return false;
        } else if (mode != null) {
            txnMode = newMode;
            txnModeChanged = false;
        }

        if (getLogger().isLoggable(Level.FINE)) {
            getLogger().fine("Receiving SessionID: " + sessionID + ", TxnID: "
                + txnID + ", TxnMode: " + txnMode);
        }
        return true;
    }

    private boolean isTxnCompatible() {
        if (compatibleTxnMode == true) {
            return true;
        }
        if (serverVersion == null) {
            return true;
        }
        // parse server version
        String[] vers = serverVersion.split("\\.");
        if (vers.length < 2) { // unexpected
            return false;
        }
        int majorVer = Integer.parseInt(vers[0]);
        if (majorVer > 8) {
            return true;
        }
        if (majorVer < 8) {
            return false;
        }
        // parse maintenance version       
    
        // assume 8.0 nightly is compatible, e.g. 8.0-20150317
        if (serverVersion.startsWith("8.0-1")) { 
            return false;
        }
        return true;
    }

    private TransactionMode parseTransactionMode(String val) {
        if(val.equals("auto")) return TransactionMode.AUTO;
        else if(val.equals("query")) return TransactionMode.QUERY;
        else if(val.equals("update")) return TransactionMode.UPDATE;
        else if (val.equals("update-auto-commit")) 
            return TransactionMode.UPDATE_AUTO_COMMIT;
        return TransactionMode.AUTO;
    }

    // ---------------------------------------------------------------

    private void assertSessionOpen() {
        if (isClosed()) {
            throw new IllegalStateException("Session has been closed");
        }
    }

    private void throwIllegalArg(String msg, Logger logger) {
        logger.severe(msg);
        throw new IllegalArgumentException(msg);
    }

    void throwIllegalState(String msg) {
        getLogger().severe(msg);
        throw new IllegalStateException(msg);
    }

    // ---------------------------------------------------------------

    @Override
    public String toString() {
        return credentials.toString() + ", cb=" + ((contentBase == null) ? "{default}" : contentBase)
                + " [ContentSource: " + contentSource.toString() + "]";
    }

    @Override
    public int getCachedTxnTimeout() {
        return timeout;
    }
}
