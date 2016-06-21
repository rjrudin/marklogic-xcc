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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.marklogic.io.IOHelper;
import com.marklogic.xcc.Request;
import com.marklogic.xcc.RequestOptions;
import com.marklogic.xcc.Session;
import com.marklogic.xcc.ValueFactory;
import com.marklogic.xcc.exceptions.UnimplementedFeatureException;
import com.marklogic.xcc.types.ValueType;
import com.marklogic.xcc.types.XName;
import com.marklogic.xcc.types.XdmSequence;
import com.marklogic.xcc.types.XdmValue;
import com.marklogic.xcc.types.XdmVariable;

public abstract class RequestImpl implements Request {
    private final Session session;
    private RequestOptions options;
    private Set<XdmVariable> variables = Collections.synchronizedSet(new LinkedHashSet<XdmVariable>());
    private long position = 1; // beginning position of the requested subsequence
    private long count = Long.MAX_VALUE; // count of the requested subsequence
    
    // ---------------------------------------------------

    public RequestImpl(Session session, RequestOptions options) {
        this.session = session;

        setOptions(options);
    }

    // ---------------------------------------------------

    abstract void urlEncodeXQueryString(StringBuffer sb, Logger logger);

    abstract String serverPath();

    abstract String requestVar();

    // ---------------------------------------------------

    public Session getSession() {
        return (session);
    }

    public void setOptions(RequestOptions options) {
        if (options == null) {
            this.options = new RequestOptions();
        } else {
            this.options = options;
        }
    }

    public RequestOptions getOptions() {
        return options;
    }

    public RequestOptions getEffectiveOptions() {
        RequestOptions eff = new RequestOptions();
        RequestOptions req = getOptions();
        RequestOptions ses = getSession().getDefaultRequestOptions();

        eff.applyEffectiveValues(new RequestOptions[] { ses, req });

        return eff;
    }

    public void setVariable(XdmVariable variable) {
        XdmValue value = variable.getValue();

        if (value instanceof XdmSequence<?>) {
            throw new UnimplementedFeatureException("Setting variables that are sequences is not supported");
        }
        
        synchronized (variables) {
            // "set" implies replacing a var with the same XName, add() doesn't replace
            clearVariable(variable);
            variables.add(variable);
        }
    }

    public XdmVariable setNewVariable(XName xname, XdmValue value) {
        XdmVariable variable = ValueFactory.newVariable(xname, value);

        setVariable(variable);

        return (variable);
    }

    public XdmVariable setNewVariable(String namespace, String localname, ValueType type, Object value) {
        return setNewVariable(new XName(namespace, localname), ValueFactory.newValue(type, value));
    }

    public XdmVariable setNewVariable(String localname, ValueType type, Object value) {
        return setNewVariable(null, localname, type, value);
    }

    public XdmVariable setNewStringVariable(String namespace, String localname, String value) {
        return setNewVariable(namespace, localname, ValueType.XS_STRING, value);
    }

    public XdmVariable setNewStringVariable(String localname, String value) {
        return setNewStringVariable(null, localname, value);
    }

    public XdmVariable setNewIntegerVariable(String namespace, String localname, long value) {
        return setNewVariable(namespace, localname, ValueType.XS_INTEGER, new Long(value));
    }

    public XdmVariable setNewIntegerVariable(String localname, long value) {
        return setNewIntegerVariable(null, localname, value);
    }

    public void clearVariable(XdmVariable variable) {
        variables.remove(variable);
    }

    public void clearVariables() {
        variables.clear();
    }

    public XdmVariable[] getVariables() {
        synchronized (variables) {
            XdmVariable[] vars = new XdmVariable[variables.size()];

            variables.toArray(vars);

            return vars;
        }
    }

    // -----------------------------------------------------

    public long getPosition() {
        return position;
    }

    public void setPosition(long position) {
        this.position = position;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    String encodedQueryString(Logger logger) {
        StringBuffer sb = new StringBuffer();

        sb.append(requestVar()).append("=");
        urlEncodeXQueryString(sb, logger);

        encodeQueryOptions(sb, null);

        encodeQueryVariables(sb, logger);
        
        encodePositionRange(sb);
        
        encodeTxn(sb);

        String payload = sb.toString();

        if (logger.isLoggable(Level.FINEST)) {
            logger.finest("Eval request POST payload: '" + payload + "'");
        }

        return (payload);
    }
    
    public void encodeTxn(StringBuffer sb) {
        if (((SessionImpl)session).txnID != null) {
            sb.append("&txnid=").append(((SessionImpl)session).txnID);
        }
        if (((SessionImpl)session).isInCompatibleMode()) {
            return;
        }
        if (session.getTransactionMode() != null) {
            sb.append("&txnmode=").append(session.getTransactionMode());
        }
        if (session.getCachedTxnTimeout() != 0) {
            sb.append("&txntimelimit=").append(session.getCachedTxnTimeout());
        }
    }

    private void encodePositionRange(StringBuffer sb) {
        if (position > 1) {
            sb.append("&pos=").append(position);
        }
        if (count < Long.MAX_VALUE) {
            sb.append("&cnt=").append(count);
        }       
    }

    public void encodeQueryOptions(StringBuffer sb, RequestOptions requestOptions) {
        RequestOptions options = (requestOptions == null) ? getEffectiveOptions() : requestOptions;

        sb.append("&locale=").append(options.getLocale().toString());
        sb.append("&tzoffset=").append((options.getTimeZone().getOffset(System.currentTimeMillis())) / 1000);

        if (session.getContentBaseName() != null) {
            String dbname = session.getContentBaseName();

            if (isName(dbname)) {
                sb.append("&dbname=");
                IOHelper.urlEncodeToStringBuffer(sb, session.getContentBaseName());
            } else {
                sb.append("&dbid=").append(dbname.substring(1)); // numeric
            }
        }

        if (options.getEffectivePointInTime() != null) {
            sb.append("&timestamp=").append(options.getEffectivePointInTime().toString());
        }

        if (options.getRequestName() != null) {
            sb.append("&requestname=");
            IOHelper.urlEncodeToStringBuffer(sb, options.getRequestName());
        }

        if (options.getDefaultXQueryVersion() != null) {
            sb.append("&defaultxquery=");
            IOHelper.urlEncodeToStringBuffer(sb, options.getDefaultXQueryVersion());
        }

        if (options.getRequestTimeLimit() != -1) {
            sb.append("&timelimit=").append(options.getRequestTimeLimit());
        }
    }

    private void encodeQueryVariables(StringBuffer sb, Logger logger) {
        XdmVariable[] vars = getVariables();

        for (int i = 0; i < vars.length; i++) {
            XdmVariable var = vars[i];
            XName xname = var.getName();
            XdmValue value = var.getValue();

            sb.append("&evn").append(i).append("=");
            if (xname.getNamespace() != null) {
                sb.append(xname.getNamespace());
            }

            sb.append("&evl").append(i).append("=").append(xname.getLocalname());
            sb.append("&evt").append(i).append("=");
            IOHelper.urlEncodeToStringBuffer(sb, value.getValueType().toString());
            sb.append("&evv").append(i).append("=");
            IOHelper.urlEncodeToStringBuffer(sb, value.asString());

            // TODO: Test this output
            if (logger.isLoggable(Level.FINEST)) {
                logger.finest(" ev" + i + ": " + xname.toString() + "(" + value.getValueType() + ") "
                        + value.toString());
            }
        }
    }

    protected boolean isName(String name) {
        if (name.length() == 0)
            return true;

        if (name.charAt(0) == '#')
            return false;

        return true;
    }
}
