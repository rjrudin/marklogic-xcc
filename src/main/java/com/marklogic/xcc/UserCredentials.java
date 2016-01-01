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
package com.marklogic.xcc;

/**
 * This interface encapsulates a user identity. It is intended primarily for internal use.
 */
public interface UserCredentials {
    /**
     * The user name associated with this credential object.
     * 
     * @return The user name as a String.
     */
    String getUserName();

    /**
     * Returns an HTTP basic authentication string.
     * 
     * @return An HTTP basic authentication header value.
     */
    String toHttpBasicAuth();
    
    /**
     * Returns an HTTP digest authentication string.
     *
     * @return An HTTP digest authentication header value.
     */
    String toHttpDigestAuth(String method, String uri, String challengeHeader);
}
