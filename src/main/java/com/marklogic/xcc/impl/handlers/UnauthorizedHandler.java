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
import com.marklogic.xcc.UserCredentials;
import com.marklogic.xcc.exceptions.RequestException;
import com.marklogic.xcc.exceptions.RequestPermissionException;
import com.marklogic.xcc.exceptions.ServerResponseException;
import com.marklogic.xcc.impl.ContentSourceImpl;

public class UnauthorizedHandler implements ResponseHandler {
    public Object handleResponse(HttpChannel http, int responseCode, Request request, Object attachment, Logger logger)
            throws RequestException {
        String challenge;

        try {
            challenge = http.getResponseHeader("www-authenticate");
        } catch (IOException e) {
            throw new ServerResponseException("Failed checking authenticate header.", request, responseCode,
                    getResponseMessage(http), e);
        }

        ContentSourceImpl contentSource = (ContentSourceImpl)request.getSession().getContentSource();

        contentSource.setAuthChallenge(challenge);
        
        String requestAuth = http.getRequestHeader("Authorization");

        boolean retryAdvised = 
            !contentSource.isChallengeIgnored() &&
            !contentSource.isAuthenticationPreemptive() &&
            ((requestAuth == null) || !challenge.regionMatches(true, 0, requestAuth, 0, 6));

        UserCredentials credentials = request.getSession().getUserCredentials();
        String userName = credentials.getUserName();
        String message = "Authorization failed for user '" + userName + "'.";

        throw new RequestPermissionException(message, request, userName, retryAdvised);
    }

    private String getResponseMessage(HttpChannel http) {
        try {
            return http.getResponseMessage();
        } catch (IOException e) {
            return ("No Message");
        }
    }
}
