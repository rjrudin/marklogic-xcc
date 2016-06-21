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

import com.marklogic.xcc.Content;
import com.marklogic.xcc.Request;

/**
 * This is a specialization of {@link ContentInsertException} that indicates the insertion faled
 * because an XML entity embedded in the document could not be resolved. Entity resolution is not
 * done by default, is must be enabled in the options (
 * {@link com.marklogic.xcc.ContentCreateOptions}) associated with the insertion request. Entity
 * resolution failures are not retryable.
 */
public class ContentInsertEntityException extends ContentInsertException {
    private static final long serialVersionUID = 9138223923804613836L;
    private final String entityLocation;

    public ContentInsertEntityException(String message, Request request, Content content, String entityLocation) {
        super(message, request, content);

        this.entityLocation = entityLocation;
    }

    public ContentInsertEntityException(String message, Request request, Content content, String entityLocation,
            Throwable cause) {
        super(message, request, content, cause);

        this.entityLocation = entityLocation;
    }

    /**
     * Returns the location, as a URI String, of the requested entity.
     * 
     * @return A URI String.
     */
    public String getEntityLocation() {
        return entityLocation;
    }
}
