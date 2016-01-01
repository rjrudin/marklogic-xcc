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
package com.marklogic.xcc.impl.handlers;

import java.io.IOException;
import java.util.logging.Logger;

import com.marklogic.http.BMBoundaryPartSplitter;
import com.marklogic.http.HttpChannel;
import com.marklogic.http.HttpHeaders;
import com.marklogic.http.MultipartBuffer;
import com.marklogic.http.MultipartSplitter;
import com.marklogic.http.NullPartSplitter;
import com.marklogic.xcc.Request;
import com.marklogic.xcc.RequestOptions;
import com.marklogic.xcc.impl.CachedResultSequence;
import com.marklogic.xcc.impl.SessionImpl;
import com.marklogic.xcc.impl.StreamingResultSequence;
import com.marklogic.xcc.impl.RequestImpl;
import com.marklogic.xcc.spi.ServerConnection;
import com.marklogic.xcc.exceptions.RequestException;

public class GoodQueryResponseHandler implements ResponseHandler {
    public Object handleResponse(HttpChannel http, int responseCode, Request request, Object attachment, Logger logger)
            throws RequestException, IOException {
        RequestOptions options = request.getEffectiveOptions();
        String boundary = http.getResponseContentBoundary();
        MultipartSplitter splitter = (boundary == null) ? (MultipartSplitter)new NullPartSplitter()
                : new BMBoundaryPartSplitter(http.getResponseStream(), boundary.getBytes(), options
                        .getResultBufferSize(), logger);
        MultipartBuffer mbuf = new MultipartBuffer(splitter);

        if (options.getCacheResult()) {
            logger.fine("ResultSequence is to be cached, reading");
            return new CachedResultSequence(request, mbuf, options);
        }

        logger.fine("ResultSequence is streaming");
        return new StreamingResultSequence(request, (ServerConnection)attachment, mbuf,
                options, logger);
    }
}
