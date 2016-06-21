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
import java.io.InputStream;

public class PartInputStream extends InputStream {
    private MultipartSplitter splitter;

    /**
     * Create a new <code>PartInputStream</code> backed by a given (shared)
     * <code>MultipartSplitter</code>. This input stream will return bytes until the underlying
     * <code>MultipartSplitter</code> detects an end-of-part boundary, in which case the
     * <code>PartInputStream</code> returns a length of -1 as an EOF indicator.
     */
    public PartInputStream(MultipartSplitter splitter) {
        this.splitter = splitter;
    }

    // implement InputStream

    /*
     * Returns the number of bytes that can be read (or skipped over) from this input stream without
     * blocking by the next caller of a method for this input stream.
     */
    @Override
    public int available() {
        return (0);
    }

    /**
     * Closes this input stream and releases any system resources associated with the stream.
     */
    @Override
    public void close() {
        splitter = null;
    }

    /**
     * Marks the current position in this input stream.
     */
    @Override
    public void mark(int readlimit) {
        throw new UnsupportedOperationException();
    }

    /**
     * Tests if this input stream supports the mark and reset methods.
     */
    @Override
    public boolean markSupported() {
        return false;
    }

    /**
     * Reads the next byte of data from the input stream.
     */
    @Override
    public int read() throws IOException {
        return splitter.read();
    }

    /**
     * Reads some number of bytes from the input stream and stores them into the buffer array b.
     */
    @Override
    public int read(byte[] buf) throws IOException {
        return read(buf, 0, buf.length);
    }

    /**
     * Reads up to <code>len</code> bytes of data from the input stream into an array of bytes. An
     * attempt is made to read as many as <code>len</code> bytes, but a smaller number may be read,
     * possibly zero. The number of bytes actually read is returned as an integer.
     * <p/>
     * <p>
     * This method blocks until input data is available, end of file is detected, or an exception is
     * thrown.
     * <p/>
     * <p>
     * If <code>buf</code> is <code>null</code>, a <code>NullPointerException</code> is thrown.
     * <p/>
     * <p>
     * If <code>offset</code> is negative, or <code>len</code> is negative, or
     * <code>offset+len</code> is greater than the length of the array <code>buf</code>, then an
     * <code>IndexOutOfBoundsException</code> is thrown.
     * <p/>
     * <p>
     * If <code>len</code> is zero, then no bytes are read and <code>0</code> is returned;
     * otherwise, there is an attempt to read at least one byte. If no byte is available because the
     * stream is at end of file, the value <code>-1</code> is returned; otherwise, at least one byte
     * is read and stored into <code>buf</code>.
     * <p/>
     * <p>
     * In every case, elements <code>buf[0]</code> through <code>buf[offset]</code> and elements
     * <code>buf[offset+len]</code> through <code>buf[buf.length-1]</code> are unaffected.
     * <p/>
     * <p>
     * If the first byte cannot be read for any reason other than end of file, then an
     * <code>IOException</code> is thrown. In particular, an <code>IOException</code> is thrown if
     * the input stream has been closed.
     * 
     * @param buf
     *            - the buffer into which the data is read.
     * @param offset
     *            - the start offset in array <code>b</code> at which the data is written.
     * @param len
     *            - the maximum number of bytes to read.
     * @return the total number of bytes read into the buffer, or <code>-1</code> if there is no
     *         more data because the end of the stream has been reached.
     * @throws IOException
     *             - if an I/O error occurs.
     */
    @Override
    public int read(byte[] buf, int offset, int len) throws IOException {
        if (splitter == null) {
            throw new IOException("Splitter stream closed");
        }

        if (offset < 0 || len < 0 || offset + len > buf.length) {
            throw new IndexOutOfBoundsException("offset or length error");
        }

        return (splitter.read(buf, offset, len));
    }

    /**
     * Repositions this stream to the position at the time the mark method was last called on this
     * input stream.
     */
    @Override
    public void reset() {
        throw new UnsupportedOperationException();
    }

    // FIXME: Check this logic, should pass through to splitter?
    /**
     * Skips over and discards n bytes of data from this input stream.
     */
    @Override
    public long skip(long n) throws IOException {
        // Not 1<<31; int only goes up to 2^31-1
        if (n < (1 << 30)) {
            byte[] buf = new byte[(int)n];
            return read(buf, 0, (int)n);
        }

        byte[] buf = new byte[1 << 30];
        long resultLen = 0;
        long residualLen = n;

        for (int m = (1 << 30); m < n; m += (1 << 30), residualLen -= (1 << 30)) {
            resultLen += read(buf, 0, (residualLen < (1 << 30) ? (int)residualLen : (1 << 30)));
        }

        return resultLen;
    }
}
