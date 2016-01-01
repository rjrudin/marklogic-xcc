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

/**
 * This exception indicates that an unrecognized response was received from 
 * the server while parsing the HTTP header of the message.  This can be caused
 * by a connection error or server timeout.
 */
public class UnexpectedResponseException extends XccException {

    private static final long serialVersionUID = 1L;
    String responseValue;
    
    public UnexpectedResponseException(String message, String responseValue) {
        super(message);
        this.responseValue = responseValue;
    }
}
