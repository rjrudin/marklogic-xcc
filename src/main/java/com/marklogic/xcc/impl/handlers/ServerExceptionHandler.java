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
import java.util.logging.Logger;

import com.marklogic.http.HttpChannel;
import com.marklogic.xcc.Request;
import com.marklogic.xcc.exceptions.RequestException;
import com.marklogic.xcc.exceptions.ServerConnectionException;
import com.marklogic.xcc.types.XdmNode;
import com.marklogic.xcc.types.impl.TextImpl;

public class ServerExceptionHandler implements ResponseHandler {
    public Object handleResponse(HttpChannel http, int responseCode, Request request, Object attachment, Logger logger)
            throws RequestException {
        try {
            XdmNode errorNode = new TextImpl(http.getResponseStream());
            RequestException ex = ServerErrorParser.makeException(request, errorNode.asString());
            Exception e = new Exception("dummy");

            // report stack trace from here, not from inside SAX parser
            ex.setStackTrace(e.getStackTrace());

            throw ex;
        } catch (IOException e) {
            throw new ServerConnectionException("Trouble parsing server error response: " + e, request, e);
        }
    }
}
