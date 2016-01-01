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
package com.marklogic.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * An InputStream decorator that returns EOF after a specified number of bytes have been read from
 * the underlying stream. If EOF is encountered on the underlying stream before the read limit is
 * hit, EOF is returned at that point.
 */
public class LengthLimitedInputStream extends InputStream {
    private InputStream stream;
    private boolean limitReached = false;
    private long limit;
    private long readCount = 0;

    public LengthLimitedInputStream(InputStream stream, long limit) {
        this.stream = stream;
        this.limit = (limit < 0) ? Long.MAX_VALUE : limit;
    }

    @Override
    public int read() throws IOException {
        if (checkLimit()) {
            return (-1);
        }

        int rc = stream.read();

        if (rc != -1) {
            advanceReadCount(1);
        }

        return (rc);
    }

    @Override
    public int read(byte buffer[], int off, int len) throws IOException {
        if (checkLimit()) {
            return (-1);
        }

        int toRead = (int)maxReadable(len);
        int rc = stream.read(buffer, off, toRead);

        if (rc <= 0) {
            return (rc);
        }

        advanceReadCount(rc);

        return (rc);
    }

    @Override
    public int read(byte buffer[]) throws IOException {
        return read(buffer, 0, buffer.length);
    }

    @Override
    public void close() throws IOException {
        limitReached = true;

        stream.close();
    }

    @Override
    public long skip(long n) throws IOException {
        long toSkip = maxReadable(n);
        long skipped = super.skip(toSkip);

        advanceReadCount(skipped);

        return (skipped);
    }

    // ------------------------------------------------------------

    private void advanceReadCount(long n) {
        readCount += n;

        checkLimit();
    }

    private long maxReadable(long n) {
        return (Math.min(n, limit - readCount));
    }

    private boolean checkLimit() {
        if (limitReached) {
            return (true);
        }

        if (readCount == limit) {
            limitReached = true;
        }

        return (limitReached);
    }
}
