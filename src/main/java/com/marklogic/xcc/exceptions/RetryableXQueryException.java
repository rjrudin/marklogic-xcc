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
 * A specialization of {@link XQueryException} which is always retryable, to define a distinct
 * exception type that can be caught separately. Note that in most cases, retryable exceptions will
 * be retried automatically.
 * 
 * @see com.marklogic.xcc.RequestOptions#setMaxAutoRetry(int)
 */
public class RetryableXQueryException extends RetryableQueryException {
    
    private static final long serialVersionUID = -4495822742402772526L;
    private final String xqueryVersion; 

    public RetryableXQueryException(Request request, String code, 
            String w3cCode, String xqueryVersion, String message,
            String formatString, String expr, boolean retryable, 
            String[] data, QueryStackFrame[] stack) {
        super(request, code, w3cCode, message, formatString, expr, retryable, 
                data, stack);
        this.xqueryVersion = xqueryVersion;
    }  
    
    /**
     * Returns the XQuery version (0.9-ml, 1.0-ml or 1.0) of the module that 
     * threw the exception.
     * 
     * @return A String that represents the XQuery version.
     */
    public String getXQueryVersion() {
        return xqueryVersion;
    }
}
