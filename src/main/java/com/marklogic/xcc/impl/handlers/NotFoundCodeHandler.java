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
import com.marklogic.xcc.ModuleInvoke;
import com.marklogic.xcc.ModuleSpawn;
import com.marklogic.xcc.Request;
import com.marklogic.xcc.exceptions.RequestException;
import com.marklogic.xcc.exceptions.ServerResponseException;

public class NotFoundCodeHandler implements ResponseHandler {
    public Object handleResponse(HttpChannel http, int responseCode, Request request, Object attachment, Logger logger)
            throws RequestException {
        String responseMsg = getResponseMessage(http);
        String queryType = "Query evaluation";

        if (request instanceof ModuleInvoke)
            queryType = "Module invocation";
        if (request instanceof ModuleSpawn)
            queryType = "Module spawn";

        throw new ServerResponseException(queryType + " request rejected (" + responseCode + ", " + responseMsg
                + "). Is this an XDBC server?", request, responseCode, getResponseMessage(http));
    }

    private String getResponseMessage(HttpChannel http) {
        try {
            return http.getResponseMessage();
        } catch (IOException e) {
            return ("No Message");
        }
    }
}
