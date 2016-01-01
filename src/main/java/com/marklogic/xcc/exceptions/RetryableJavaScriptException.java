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
 * A server exception resulting from an error encountered while evaluating 
 * JavaScript. Note that some such exceptions are retryable.
 * 
 * @see #isRetryable()
 * @see RetryableXQueryException
 */
public class RetryableJavaScriptException extends RetryableQueryException {

    private static final long serialVersionUID = -193437387273398256L;

    public RetryableJavaScriptException(Request request, String code,
            String w3cCode, String message, String formatString, String expr,
            boolean retryable, String[] data, QueryStackFrame[] stack) {
        super(request, code, w3cCode, message, formatString, expr, retryable, 
            data, stack);
    }
}
