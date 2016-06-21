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
package com.marklogic.xcc.exceptions;

import com.marklogic.xcc.Request;
import com.marklogic.xcc.Version;
import com.marklogic.xcc.impl.SessionImpl;

/**
 * The base class for exceptions related to submitting requests to the server.
 */
public class RequestException extends XccException {
    private static final long serialVersionUID = -7193834172330203276L;
    private transient Request request;

    public RequestException(String message, Request request) {
        super(message);

        this.request = request;
    }

    public RequestException(String message, Request request, Throwable cause) {
        super(message, cause);

        this.request = request;
    }

    /**
     * The initiating request to which this exception applies.
     * 
     * @return An instance of {@link Request}.
     */
    public Request getRequest() {
        return request;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer(super.toString());
        appendRequestInfo(sb);
        return sb.toString();
    }
    
    protected void appendRequestInfo(StringBuffer sb) {
        String nl = System.getProperty("line.separator");
        String serverVersion = ((SessionImpl)request.getSession()).getServerVersion();
        sb.append(nl);
        sb.append(" [Session: ");
        sb.append(request.getSession().toString());
        sb.append("]");
        sb.append(nl);
        sb.append(" [Client: XCC/");
        sb.append(Version.getVersionString());
        if (serverVersion != null) {
            sb.append(", Server: XDBC/");
            sb.append(serverVersion);
        }
        sb.append("]");
    }
}
