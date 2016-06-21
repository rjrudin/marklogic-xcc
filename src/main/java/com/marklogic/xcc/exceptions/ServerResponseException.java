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

/**
 * This exception indicates that an unrecognized response was received from the server. This may
 * indicate a misconfiguration such that XCC is connecting to something other than a MarkLogic XDBC
 * server port.
 */
public class ServerResponseException extends ServerConnectionException {
    private static final long serialVersionUID = -4888769581702598169L;
    private int responseCode;
    private String responseMessage;

    public ServerResponseException(String message, Request request, int responseCode, String responseMessage) {
        super(message, request);

        this.responseCode = responseCode;
        this.responseMessage = responseMessage;
    }

	public ServerResponseException (String message, Request request, int responseCode, String responseMessage, Throwable cause)
	{
		super (message, request, cause);

		this.responseCode = responseCode;
		this.responseMessage = responseMessage;
	}

    public int getResponseCode() {
        return responseCode;
    }

    public String getResponseMessage() {
        return responseMessage;
    }
}
