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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.logging.Level;
import java.math.BigInteger;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import javax.transaction.xa.XAException;

import com.marklogic.xcc.exceptions.RequestException;
import com.marklogic.xcc.types.ValueType;
import com.marklogic.xcc.types.XSInteger;
import com.marklogic.xcc.types.XSHexBinary;
import com.marklogic.xcc.ResultItem;
import com.marklogic.xcc.ResultSequence;
import com.marklogic.xcc.exceptions.ServerConnectionException;
import com.marklogic.xcc.exceptions.RequestPermissionException;
import com.marklogic.xcc.exceptions.XQueryException;
import com.marklogic.xcc.exceptions.RetryableXQueryException;
import com.marklogic.xcc.types.impl.XsHexBinaryImpl;

public class XAResourceImpl implements XAResource {

    // An implementation of Xid with an equals() and hashCode() method
    // that we can rely on.
    private static class XccXid implements Xid {
        private int format;
        private byte[] gtid;
        private byte[] bq;

        public XccXid(int f, byte[] g, byte[] b) {
            this.format = f;
            this.gtid = g;
            this.bq = b;
        }
        public XccXid(Xid o) {
            this.format = o.getFormatId();
            this.gtid = o.getGlobalTransactionId();
            this.bq = o.getBranchQualifier();
        }

        public int getFormatId() { return format; }

        public byte[] getGlobalTransactionId() { return gtid; }

        public byte[] getBranchQualifier() { return bq; }

        @Override
        public boolean equals(Object o) {
            return o instanceof Xid &&
                format == ((Xid)o).getFormatId() &&
                Arrays.equals(gtid, ((Xid)o).getGlobalTransactionId()) &&
                Arrays.equals(bq, ((Xid)o).getBranchQualifier());
        }

        @Override
        public int hashCode() {
            int hash = format;
            for(byte b : gtid) hash += b;
            for(byte b : bq) hash += b;
            return hash;
        }

        @Override
        public String toString() {
            return "< format=" + format +
                ", gtid=" + XsHexBinaryImpl.convertBinaryToHex(gtid) +
                ", bq=" + XsHexBinaryImpl.convertBinaryToHex(bq) + " >";
        }
    }

    private SessionImpl session;
    private XAResourceImpl joined = null;
    private boolean origCompatibleMode = false;

    public XAResourceImpl(SessionImpl session) {
        this.session = session;
    }

    private static final BigInteger bigZero = new BigInteger("0");

    private static final Map<Xid, BigInteger> coordinatorForestMap = Collections
        .synchronizedMap(new HashMap<Xid, BigInteger>());
    private static final Map<Xid, XAResourceImpl> xaMap = Collections
        .synchronizedMap(new HashMap<Xid, XAResourceImpl>());

    private static AdhocImpl createAdhoc(SessionImpl session, String query, Xid xid, boolean doForestID)
    {
        AdhocImpl adhoc = new AdhocImpl(session,
            "xquery version '1.0-ml';\n" +
            "import module namespace xa='http://marklogic.com/xdmp/xa' at 'MarkLogic/xa.xqy';\n" +
            "declare variable $f as xs:integer external;\n" +
            "declare variable $g as xs:hexBinary external;\n" +
            "declare variable $b as xs:hexBinary external;\n" +
            "declare variable $xid as element(xa:xid) := xa:make-xid($f,$g,$b);\n" +
            (!doForestID ? "" :
                "declare variable $fid as xs:integer external;\n" +
                "declare variable $forest as xs:unsignedLong? :=\n" +
                "  if($fid eq 0) then () else xs:unsignedLong($fid);\n") +
            query, null);
        adhoc.setNewIntegerVariable("f",xid.getFormatId());
        adhoc.setNewVariable("g",ValueType.XS_HEX_BINARY,xid.getGlobalTransactionId());
        adhoc.setNewVariable("b",ValueType.XS_HEX_BINARY,xid.getBranchQualifier());
        if(doForestID) {
            BigInteger forestID = coordinatorForestMap.get(new XccXid(xid));
            if(forestID==null) forestID = bigZero;
            adhoc.setNewVariable("fid",ValueType.XS_INTEGER,forestID);
        }
        return adhoc;
    }

    private void handleException(RequestException e) throws XAException {
        int code = XAException.XAER_RMERR;
        if(e instanceof ServerConnectionException)
            code = XAException.XAER_RMFAIL;
        else if(e instanceof RequestPermissionException)
            code = XAException.XAER_INVAL;
        else if(e instanceof RetryableXQueryException)
            code = XAException.XA_RBTRANSIENT;
        else if(e instanceof XQueryException) {
            XQueryException xe = (XQueryException)e;
            if(xe.getCode().equals("XDMP-TIMELIMIT") || xe.getCode().equals("XDMP-CANCELED"))
                code = XAException.XA_RBTIMEOUT;
            else if(xe.getCode().equals("XDMP-ROLLBACK"))
                code = XAException.XA_RBROLLBACK;
            else if(xe.getCode().equals("XDMP-DEADLOCK"))
                code = XAException.XA_RBDEADLOCK;
            else if(xe.getCode().equals("XDMP-NOTXN"))
                code = XAException.XA_RBOTHER;
            else if(xe.getCode().equals("XDMP-READONLY"))
                code = XAException.XA_RDONLY;
            else if(xe.getCode().equals("XDMP-HEURCOM"))
                code = XAException.XA_HEURCOM;
            else if(xe.getCode().equals("XDMP-HEURRB"))
                code = XAException.XA_HEURRB;
            else if(xe.getCode().equals("XDMP-DUPXID"))
                code = XAException.XAER_DUPID;
            else if(xe.getCode().equals("XDMP-XIDNOTFOUND"))
                code = XAException.XAER_NOTA;
            else if(xe.getCode().equals("XDMP-OWNTXN") || xe.getCode().equals("XDMP-NOTPREPARED") ||
                xe.getCode().equals("XDMP-TXNCOMPLETED") || xe.getCode().equals("XDMP-NOTREMEMBERED") ||
                xe.getCode().equals("XDMP-NOTCURRENT"))
                code = XAException.XAER_PROTO;
        }

        if(code < 0) {
            // It's an XAER_* code
            session.getLogger().log(Level.WARNING, "XAResource error condition", e);
        } else {
            session.getLogger().log(Level.INFO, "XAResource exception", e);
        }

        XAException toThrow = new XAException(code);
        toThrow.initCause(e);
        throw toThrow;
    }

    private static void handleFinally(ResultSequence rs, SessionImpl session) {
        if(rs!=null) rs.close();
        if(session!=null) session.close();
    }

    public synchronized void start(Xid xid, int flags) throws XAException {
        origCompatibleMode = session.isInCompatibleMode();
        session.setCompatibleMode(true);
        session.getLogger().fine("XAResource.start, xid=" + xid.toString() + ", flags=" + flags);

        if(session.getTxnID() != null)
            throw new XAException(XAException.XAER_OUTSIDE);

        boolean join = flags==TMJOIN || flags==TMRESUME;
        try {
            AdhocImpl adhoc = createAdhoc(session,join?"xa:join($xid)":"xa:start($xid)",xid,false);
            session.submitRequestInternal(adhoc).close();
            synchronized(session) {
                session.inXATxn = true;
            }
            if(join) {
                XAResourceImpl other = xaMap.get(new XccXid(xid));
                if(other!=null && other!=this) {
                    synchronized(other) {
                        joined = other.joined;
                        other.joined = this;
                    }
                } else {
                    xaMap.put(new XccXid(xid), this);
                }
            } else {
                xaMap.put(new XccXid(xid), this);
            }
        } catch(RequestException e) { handleException(e); }
    }

    public synchronized void end(Xid xid, int flags) throws XAException {
        session.getLogger().fine("XAResource.end, xid=" + xid.toString() + ", flags=" + flags);

        try {
            AdhocImpl adhoc = createAdhoc(session,"xa:end($xid)",xid,false);
            session.submitRequestInternal(adhoc).close();
        } catch(RequestException e) {
            // Ignore XDMP-NOTCURRENT (effectively the end already succeeded)
            if(!(e instanceof XQueryException) || !((XQueryException)e).getCode().equals("XDMP-NOTCURRENT"))
                handleException(e);
        }
        if(joined!=null) {
            joined.ended();
            joined = null;
        }
        xaMap.remove(new XccXid(xid));
        session.setCompatibleMode(origCompatibleMode);
    }

    protected synchronized void ended() {
        if(joined!=null) {
            joined.ended();
            joined = null;
        }
        synchronized(session) {
            session.inXATxn = false;
            session.txnID = null;
            session.sessionID = null;
        }
    }

    public int prepare(Xid xid) throws XAException {
        SessionImpl backChannel = session.clone();
        ResultSequence rs = null;
        try {
            backChannel.getLogger().fine("XAResource.prepare, xid=" + xid.toString());

            AdhocImpl adhoc = createAdhoc(backChannel,"xa:prepare($xid)",xid,false);
            rs = backChannel.submitRequestInternal(adhoc);
            BigInteger forestID = ((XSInteger)rs.next().getItem()).asBigInteger();
            if(forestID.compareTo(bigZero) == 0) return XA_RDONLY;
            // Store the forest ID, so that it can be used later
            coordinatorForestMap.put(new XccXid(xid), forestID);
        } catch(RequestException e) {
            handleException(e);
        } finally {
            handleFinally(rs,backChannel);
        }
        return XA_OK;
    }

    public void commit(Xid xid, boolean onePhase) throws XAException {
        SessionImpl backChannel = session.clone();
        try {
            backChannel.getLogger().fine("XAResource.commit, xid=" + xid.toString() + ", onePhase=" + onePhase);

            AdhocImpl adhoc = createAdhoc(backChannel,"xa:commit($xid,$forest)",xid,true);
            backChannel.submitRequestInternal(adhoc).close();
            coordinatorForestMap.remove(new XccXid(xid));
        } catch(RequestException e) {
            handleException(e);
        } finally {
            handleFinally(null,backChannel);
        }
    }

    public void rollback(Xid xid) throws XAException {
        SessionImpl backChannel = session.clone();
        try {
            backChannel.getLogger().fine("XAResource.rollback, xid=" + xid.toString());

            AdhocImpl adhoc = createAdhoc(backChannel,"xa:rollback($xid,$forest)",xid,true);
            backChannel.submitRequestInternal(adhoc).close();
            coordinatorForestMap.remove(new XccXid(xid));
        } catch(RequestException e) {
            handleException(e);
        } finally {
            handleFinally(null,backChannel);
        }
    }

    public void forget(Xid xid) throws XAException {
        SessionImpl backChannel = session.clone();
        try {
            backChannel.getLogger().fine("XAResource.forget, xid=" + xid.toString());

            AdhocImpl adhoc = createAdhoc(backChannel,"xa:forget($xid,$forest)",xid,true);
            backChannel.submitRequestInternal(adhoc).close();
            coordinatorForestMap.remove(new XccXid(xid));
        } catch(RequestException e) {
            handleException(e);
        } finally {
            handleFinally(null,backChannel);
        }
    }

    public Xid[] recover(int flag) throws XAException {
        ArrayList<Xid> result = new ArrayList<Xid>();
        SessionImpl backChannel = session.clone();
        ResultSequence rs = null;
        try {
            backChannel.getLogger().fine("XAResource.recover, flag=" + flag);
            if(flag!=TMSTARTRSCAN) return new Xid[0];

            AdhocImpl adhoc = new AdhocImpl(backChannel,
                "xquery version '1.0-ml';\n" +
                "import module namespace xa='http://marklogic.com/xdmp/xa' at 'MarkLogic/xa.xqy';\n" +
                "for $xid in xa:recover() return (\n" +
                "  $xid/@format-id cast as xs:integer,\n" +
                "  $xid/xa:global-transaction-id cast as xs:hexBinary,\n" +
                "  $xid/xa:branch-qualifier cast as xs:hexBinary)", null);
            rs = backChannel.submitRequestInternal(adhoc);

            ResultItem item;
            while((item = rs.next()) != null) {
                if(!(item.getItem() instanceof XSInteger)) {
                    backChannel.getLogger().severe("XAResourceImpl.recover() expecting XSInteger");
                    break;
                }
                int format = ((XSInteger)item.getItem()).asPrimitiveInt();
                if((item = rs.next()) == null || !(item.getItem() instanceof XSHexBinary)) {
                    backChannel.getLogger().severe("XAResourceImpl.recover() expecting XSHexBinary");
                    break;
                }
                byte[] gtid = ((XSHexBinary)item.getItem()).asBinaryData();
                if((item = rs.next()) == null || !(item.getItem() instanceof XSHexBinary)) {
                    backChannel.getLogger().severe("XAResourceImpl.recover() expecting XSHexBinary");
                    break;
                }
                byte[] bq = ((XSHexBinary)item.getItem()).asBinaryData();
                result.add(new XccXid(format, gtid, bq));
            }
        } catch(RequestException e) {
            handleException(e);
        } finally {
            handleFinally(rs,backChannel);
        }

        return result.toArray(new Xid[0]);
    }

    public boolean isSameRM(XAResource xares) throws XAException {
        if(xares instanceof XAResourceImpl) {
            XAResourceImpl other = (XAResourceImpl)xares;

            // Check the ContentSources come from the equal ConnectionProviders -
            // ie: they have the same host address, port, and SSL options
            if(!session.getContentSource().getConnectionProvider().equals(
                    other.session.getContentSource().getConnectionProvider()))
                return false;

            // Check the Sessions have the same user credentials
            if(!session.getUserCredentials().toHttpBasicAuth().equals(
                    other.session.getUserCredentials().toHttpBasicAuth()))
                return false;

            // Check target database of Sessions
            if(session.getContentBaseName()==null) {
                if(other.session.getContentBaseName()!=null)
                    return false;
            } else {
                if(other.session.getContentBaseName()==null ||
                    !session.getContentBaseName()
                    .equals(other.session.getContentBaseName()))
                    return false;
            }
            return true;
        }

        return false;
    }

    public int getTransactionTimeout() throws XAException {
        try {
            return session.getTransactionTimeout();
        } catch(RequestException e) {
            handleException(e);
        }
        // Never happens
        return 0;
    }

    public boolean setTransactionTimeout(int seconds) throws XAException {
        try {
            session.setTransactionTimeout(seconds);
        } catch(RequestException e) {
            handleException(e);
        }
        return true;
    }
}
