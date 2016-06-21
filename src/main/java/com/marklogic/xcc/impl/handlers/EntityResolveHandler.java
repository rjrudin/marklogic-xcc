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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Logger;

import com.marklogic.http.HttpChannel;
import com.marklogic.http.HttpHeaders;
import com.marklogic.io.IOHelper;
import com.marklogic.xcc.Content;
import com.marklogic.xcc.ContentFactory;
import com.marklogic.xcc.Request;
import com.marklogic.xcc.exceptions.ContentInsertEntityException;
import com.marklogic.xcc.exceptions.ContentInsertException;
import com.marklogic.xcc.exceptions.RequestException;

public class EntityResolveHandler implements ResponseHandler {
    public Object handleResponse(HttpChannel http, int responseCode, Request request, Object attachment, Logger logger)
            throws RequestException, IOException {
        String location = http.getResponseHeader("location");
        Content content = (Content)attachment;

        logger.fine("creating Content object to resolve entity '" + location + "' from '" + content.getUri() + "'");

        return buildEntityContent(location, request, content);
    }

    // package-local for unit testing
    ContentInsertController.ContentDecorator buildEntityContent(String location, Request request, Content content)
            throws IOException, ContentInsertException {
        try {
            Content entity = createEntityContent(request, location, content);

            return (new ContentInsertController.ContentDecorator(entity, content, location));
        } catch (URISyntaxException e) {
            throw new ContentInsertEntityException(
                    "URI requested by server for entity resolution is not valid syntax: " + location, request, content,
                    location, e);
        }
    }

    private Content createEntityContent(Request request, String location, Content parent) throws IOException,
            URISyntaxException, ContentInsertEntityException {
        if (location.startsWith("file:/")) {
            return createFileContent(request, location, parent);
        }

        if (location.startsWith("/")) {
            return createFileContent(request, "file:" + location, parent);
        }

        return ContentFactory.newContent(location, new URI(location), null);
    }

    private Content createFileContent(Request request, String location, Content parent)
            throws ContentInsertEntityException {
        String path = location.substring((location.startsWith("file:///")) ? 7 : 5);
        File file = new File(path);
        String errorMsg = null;

        if (!file.exists()) {
            file = new File(IOHelper.urlDecodeString(path));
        }

        if (!file.exists())
            errorMsg = "Entity '" + location + "' does not exist";
        if ((errorMsg == null) && (!file.isFile()))
            errorMsg = "Entity '" + location + "' exists, but is not a file";
        if ((errorMsg == null) && (!file.canRead()))
            errorMsg = "Entity '" + location + "' is not readable";

        if (errorMsg != null) {
            throw new ContentInsertEntityException(errorMsg, request, parent, location);
        }

        return ContentFactory.newContent(location, file, null);
    }
}
