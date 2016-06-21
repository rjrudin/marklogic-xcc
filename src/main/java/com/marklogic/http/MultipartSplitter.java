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
package com.marklogic.http;

import java.io.IOException;

public interface MultipartSplitter {
    public static final int DEF_BUFFER_SIZE = 16 * 1024;

    boolean hasNext() throws IOException;

    void next() throws IOException;

    int read() throws IOException;

    int read(byte[] buffer, int offset, int length) throws IOException;

    void close() throws IOException;
    
    long getTotalBytesRead();
}
