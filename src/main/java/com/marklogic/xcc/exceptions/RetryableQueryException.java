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
 * A specialization of {@link QueryException} which is always retryable, 
 * to define a distinct exception type that can be caught separately. Note 
 * that in most cases, retryable exceptions will be retried automatically.
 * 
 * @see #isRetryable()
 * @see RetryableXQueryException
 */
public class RetryableQueryException extends QueryException {

    private static final long serialVersionUID = 1931385263685178663L;

    public RetryableQueryException(Request request, String code,
            String w3cCode, String message, String formatString, String expr,
            boolean retryable, String[] data, QueryStackFrame[] stack) {
        super(request, code, w3cCode, message, formatString, expr, retryable, 
                data, stack);
    }

}
