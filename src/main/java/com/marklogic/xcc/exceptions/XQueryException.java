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
 * A server exception resulting from an error encountered while evaluating XQuery. Note that some
 * such exceptions are retryable.
 * 
 * @see #isRetryable()
 * @see RetryableXQueryException
 */
public class XQueryException extends QueryException {
    private static final long serialVersionUID = 9187877171979743149L;
    private final String xqueryVersion;    

    /**
     * Constructs an XQueryException
     * 
     * @param request
     *            The {@link com.marklogic.xcc.Request} object to which this exception applies
     * @param code
     *            the symbolic exception code
     * @param w3cCode
     *            the exception code defined by the XQuery spec, if any
     * @param xqueryVersion
     *            the XQuery version of the module that threw an exception
     * @param message
     *            the exception message
     * @param formatString
     *            the exception format string
     * @param expr
     *            The expression that caused the exception, if applicable
     * @param retryable
     *            retrying the operation may succeed
     * @param data
     *            the exception data
     * @param stack
     *            the xquery evaluator stack trace
     */
    public XQueryException(Request request, String code, String w3cCode, 
            String xqueryVersion, String message, String formatString, 
            String expr, boolean retryable, String[] data, 
            QueryStackFrame[] stack) {
        super(request, code, w3cCode, message, formatString, expr, retryable, 
                data, stack);
        this.xqueryVersion = xqueryVersion;
    }

    // ------------------------------------------------------------

    

    /**
     * Returns the XQuery version (0.9-ml, 1.0-ml or 1.0) of the module that threw the exception.
     * 
     * @return A String that represents the XQuery version.
     */
    public String getXQueryVersion() {
        return xqueryVersion;
    }

    // ----------------------------------------------------
}
