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
package com.marklogic.xcc.exceptions;

import com.marklogic.xcc.Content;
import com.marklogic.xcc.Request;

/**
 * This exception indicates a failed attempt to insert a {@link com.marklogic.xcc.Content} object.
 * Such failures are automatically retried up the number of times allowed by the request's
 * options ({@link com.marklogic.xcc.RequestOptions#setMaxAutoRetry(int)}). If this execption
 * ocurrs, either all retries have been exhausted, no retries were configured, or the specific
 * {@link com.marklogic.xcc.Content} instance was not retryable (
 * {@link com.marklogic.xcc.Content#isRewindable()}).
 */
public class ContentInsertException extends RequestServerException {
    private static final long serialVersionUID = -4061934165729146460L;
    private final transient Content content;

    public ContentInsertException(String message, Request request, Content content) {
        super(message, request);

        this.content = content;
    }

    public ContentInsertException(String message, Request request, Content content, Throwable cause) {
        super(message, request, cause);

        this.content = content;
    }

    /**
     * The {@link Content} object that could not be inserted.
     * 
     * @return an instance of {@link Content}.
     */
    public Content getContent() {
        return content;
    }

    /**
     * The URI, as a String, of the {@link com.marklogic.xcc.Content} that could not be inserted.
     * 
     * @return a URI String.
     */
    public String getContentUri() {
        return content.getUri();
    }
}
