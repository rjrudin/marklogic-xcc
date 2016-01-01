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
package com.marklogic.http;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BMBoundaryPartSplitter implements MultipartSplitter {
    private static final int MIN_BUFFER_SIZE = 2 * 1024;
    private static final int MAX_BUFFER_SIZE = 10 * 1024 * 1024;
    private static final byte[] BOUNDARY_LEADIN = "\n--".getBytes();
    private static final byte[] BOUNDARY_INTERPART_LEADOUT = "\n".getBytes();
    private static final byte[] BOUNDARY_TERMINAL_LEADOUT = "--\n".getBytes();

    private final InputStream httpStream;
    private final BoyerMoore baseBoundaryMatcher;
    private final BoyerMoore interPartBoundaryMatcher;
    private final int interPartBoundaryLength;
    private final BoyerMoore terminalBoundaryMatcher;
    private final int terminalBoundaryLength;
    private final Logger logger;
    private final byte[] bufferBytes;
    private final ByteBuffer byteBuffer;
    private final int lowWaterMark;

    private boolean streamEOS = false;
    private int readableBytes = 0;
    private boolean atTerminalBoundary = false;
    private boolean atBoundary = false;
    private long totalBytesRead = 0;

    public BMBoundaryPartSplitter(InputStream inputStream, byte[] boundary, int bufSize, Logger loggerArg)
            throws IOException {
        if (loggerArg == null) {
            logger = Logger.getLogger(getClass().getName());
        } else {
            logger = loggerArg;
        }

        httpStream = inputStream;

        int bufferSize = bufferSize(bufSize);
        bufferBytes = new byte[bufferSize];
        byteBuffer = ByteBuffer.wrap(bufferBytes);
        byteBuffer.limit(0);

        byte[] bytes = new byte[boundary.length + BOUNDARY_LEADIN.length];
        System.arraycopy(BOUNDARY_LEADIN, 0, bytes, 0, BOUNDARY_LEADIN.length);
        System.arraycopy(boundary, 0, bytes, BOUNDARY_LEADIN.length, boundary.length);
        baseBoundaryMatcher = new BoyerMoore(bytes);

        bytes = new byte[boundary.length + BOUNDARY_LEADIN.length + BOUNDARY_INTERPART_LEADOUT.length];
        System.arraycopy(BOUNDARY_LEADIN, 0, bytes, 0, BOUNDARY_LEADIN.length);
        System.arraycopy(boundary, 0, bytes, BOUNDARY_LEADIN.length, boundary.length);
        System.arraycopy(BOUNDARY_INTERPART_LEADOUT, 0, bytes, BOUNDARY_LEADIN.length + boundary.length,
                BOUNDARY_INTERPART_LEADOUT.length);
        interPartBoundaryMatcher = new BoyerMoore(bytes);
        interPartBoundaryLength = bytes.length;

        bytes = new byte[boundary.length + BOUNDARY_LEADIN.length + BOUNDARY_TERMINAL_LEADOUT.length];
        System.arraycopy(BOUNDARY_LEADIN, 0, bytes, 0, BOUNDARY_LEADIN.length);
        System.arraycopy(boundary, 0, bytes, BOUNDARY_LEADIN.length, boundary.length);
        System.arraycopy(BOUNDARY_TERMINAL_LEADOUT, 0, bytes, BOUNDARY_LEADIN.length + boundary.length,
                BOUNDARY_TERMINAL_LEADOUT.length);
        terminalBoundaryMatcher = new BoyerMoore(bytes);
        terminalBoundaryLength = bytes.length;
        lowWaterMark = terminalBoundaryLength * 2;

        if (logger.isLoggable(Level.FINER)) {
            logger.finer("Constructed: bufsize=" + bufferSize + ", boundary='" + new String(boundary) + "'");
        }

        fillBuffer();
    }

    public BMBoundaryPartSplitter(InputStream inputStream, byte[] boundary, int bufSize) throws IOException {
        this(inputStream, boundary, bufSize, null);
    }

    public long getTotalBytesRead() {
        return totalBytesRead;
    }

    private int bufferSize(int bufSize) {
        if (bufSize == 0)
            return DEF_BUFFER_SIZE;
        if (bufSize < MIN_BUFFER_SIZE)
            return MIN_BUFFER_SIZE;
        if (bufSize > MAX_BUFFER_SIZE)
            return MAX_BUFFER_SIZE;

        return bufSize;
    }

    // -----------------------------------------------------------------------------
    // MultipartSplitter interface

    public void close() throws IOException {
        // Skip to end of stream, but don't close it.
        // This stream is a "view" of the HTTP socket stream,
        // we don't want the close to propagate to the "real"
        // socket stream.

        long skipped = httpStream.skip(Long.MAX_VALUE);

        if (skipped > 0) {
            totalBytesRead += skipped;
            if (logger.isLoggable(Level.FINEST))
                logger.finest("flushed " + skipped + " bytes on close");
        }
    }

    public boolean hasNext() throws IOException {
        flushToBoundary();

        return !atTerminalBoundary;
    }

    public void next() throws IOException {
        if (!atBoundary) {
            flushToBoundary();
        }

        if (atTerminalBoundary) {
            logger.finest("at terminal boundary");
            return;
        }

        stepOverBoundary();
    }

    public int read() throws IOException {
        if (readableBytes < 1) {
            fillBuffer();
        }
        if (readableBytes < 1) {
            return -1;
        }
        else {
            int b = byteBuffer.get();
            readableBytes--;
            ++totalBytesRead;
            return b&0xff;
        }
    }
    
    public int read(byte[] buffer, int offsetArg, int length) throws IOException {
        boolean logFinest = logger.isLoggable(Level.FINEST);
        int remaining = length;
        int offset = offsetArg;
        int totalRead = 0;

        if (logFinest)
            logger.finest("enter");

        while (remaining > 0) {
            fillBuffer();

            if (readableBytes == 0) {
                if (!atBoundary) {
                    throw new IOException("Premature End-Of-Stream on read.  Server connection lost?");
                }
                if (logFinest)
                    logger.finest("readableBytes=0, break");
                break;
            }

            int rc = copyOutBytes(buffer, offset, remaining);

            remaining -= rc;
            offset += rc;
            totalRead += rc;
        }

        if (logFinest)
            logger.finest("exit: totalRead=" + totalRead);

        if (totalRead == 0) {
            return -1;
        }
        else {
            totalBytesRead += totalRead;
            return totalRead;
        }
    }

    // ------------------------------------------------------------------------

    private int copyOutBytes(byte[] buffer, int offset, int length) {
        int toCopy = (readableBytes < length) ? readableBytes : length;

        byteBuffer.get(buffer, offset, toCopy);

        readableBytes -= toCopy;

        if (logger.isLoggable(Level.FINEST)) {
            logger.finest("copied out " + toCopy + " bytes");
        }

        return toCopy;
    }

    private void fillBuffer() throws IOException {
        boolean logFinest = logger.isLoggable(Level.FINEST);

        if (streamEOS || (byteBuffer.remaining() > lowWaterMark)) {
            if (logFinest)
                logger.finest("no read: EOS=" + streamEOS + ", remain=" + byteBuffer.remaining() + ", low-water="
                        + lowWaterMark);

            if (readableBytes == 0) {
                checkForBoundary();
            }

            return;
        }

        if (logFinest)
            logger.finest("compacting buffer, remaining=" + byteBuffer.remaining());

        byteBuffer.compact();

        while ((!streamEOS) && byteBuffer.hasRemaining()) {
            int position = byteBuffer.position();
            int rc = httpStream.read(bufferBytes, position, byteBuffer.remaining());

            if (logFinest)
                logger.finest("read: rc=" + rc);

            if (rc == -1) {
                if (logFinest)
                    logger.finest("EOS, setting streamEOS flag, break");

                streamEOS = true;

                break;
            }

            byteBuffer.position(position + rc);

            if (logFinest) {
                logger.finest(" added " + rc + " bytes to buffer: pos=" + byteBuffer.position() + ", remaining="
                        + byteBuffer.remaining());
            }
        }

        byteBuffer.flip();

        checkForBoundary();
    }

    private void checkForBoundary() {
        atBoundary = atTerminalBoundary = false;

        boolean logFinest = logger.isLoggable(Level.FINEST);
        int position = byteBuffer.position();
        int limit = byteBuffer.limit();
        int boundaryPos = baseBoundaryMatcher.search(bufferBytes, position, limit);

        if (logFinest)
            logger.finest("boundaryPos=" + boundaryPos);

        if (boundaryPos == -1) {
            readableBytes = limit - baseBoundaryMatcher.partialMatch() - position;
            if (logFinest)
                logger.finest("no boundary, readableBytes=" + readableBytes);
            return;
        }

        readableBytes = boundaryPos - position;
        if (logFinest)
            logger.finest("possible boundary seen at " + boundaryPos + ", readableBytes=" + readableBytes);

        if (boundaryPos == position) {
            if (terminalBoundaryMatcher.search(bufferBytes, boundaryPos, boundaryPos + terminalBoundaryLength) == position) {
                totalBytesRead += terminalBoundaryLength;
                atTerminalBoundary = true;
                atBoundary = true;
                if (logFinest)
                    logger.finest("terminal boundary at " + boundaryPos);
            } else if (interPartBoundaryMatcher.search(bufferBytes, boundaryPos, boundaryPos + interPartBoundaryLength) == position) {
                atBoundary = true;
                if (logFinest)
                    logger.finest("inter-part boundary at " + boundaryPos);
            }
        }
    }

    private void flushToBoundary() throws IOException {
        while (!atBoundary) {
            totalBytesRead += readableBytes;
            byteBuffer.position(byteBuffer.position() + readableBytes);
            fillBuffer();
            checkForBoundary();

            if ((readableBytes == 0) && (!atBoundary)) {
                throw new IOException("Premature End-Of-Stream on flush.  Server connection lost?");
            }

        }
    }

    private void stepOverBoundary() {
        if (atTerminalBoundary) {
            throw new IllegalStateException("Attempt to step over terminal boundary");
        }

        totalBytesRead += interPartBoundaryLength;
        byteBuffer.position(byteBuffer.position() + interPartBoundaryLength);

        if (logger.isLoggable(Level.FINEST))
            logger.finest("stepped over boundary, new position=" + byteBuffer.position());

        checkForBoundary();
    }
}
