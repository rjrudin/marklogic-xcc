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

import com.marklogic.xcc.ResultSequence;

/**
 * This unchecked (runtime) exception may be thrown by some methods of streaming
 * {@link com.marklogic.xcc.ResultSequence} objects if an {@link java.io.IOException} occurs while
 * processing the streaming data.
 */
public class StreamingResultException extends RuntimeException {
    private static final long serialVersionUID = -8539650156181242034L;
    private final transient ResultSequence resultSequence;

    public StreamingResultException(String message, ResultSequence resultSequence, Throwable cause) {
        super(message, cause);

        this.resultSequence = resultSequence;
    }

    public StreamingResultException(ResultSequence resultSequence, Throwable cause) {
        super(cause);

        this.resultSequence = resultSequence;
    }

    public ResultSequence getResultSequence() {
        return resultSequence;
    }
}
