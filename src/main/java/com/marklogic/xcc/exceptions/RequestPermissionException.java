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

import com.marklogic.xcc.Request;

/**
 * This exception indicates missing or incorrect permissions for a server request.
 */
public class RequestPermissionException extends RequestException {
    private static final long serialVersionUID = 4979097514231434943L;
    private String user;
    private boolean retryAdvised = false;

    public RequestPermissionException(String message, Request request, String user) {
        super(message, request);

        this.user = user;
    }

    public RequestPermissionException(String message, Request request, String user, boolean retryAdvised) {
        super(message, request);

        this.user = user;
        this.retryAdvised = retryAdvised;
    }

    public RequestPermissionException(String message, Request request, String user, Throwable cause) {
        super(message, request, cause);

        this.user = user;
    }

    /**
     * The user to which this permission issue applies.
     * 
     * @return A user name as a String.
     */
    public String getUser() {
        return user;
    }

    /**
     * Set to true when a request is expected to succeed if retried with valid credentials.
     * 
     * @return true if the request should be retried.
     */
    public boolean isRetryAdvised() {
        return retryAdvised;
    }
}
