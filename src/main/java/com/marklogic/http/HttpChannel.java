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
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.logging.Logger;

import com.marklogic.io.LengthLimitedInputStream;
import com.marklogic.io.SslByteChannel;

public class HttpChannel {
    // IN CASE OF EMERGENCY BREAK GLASS
    private static boolean useHTTP = "true".equalsIgnoreCase(System.getProperty("xcc.httpcompliant"));
    
    public static final String RCV_TIME_HEADER = "X-XCC-Received";

    static final int DEFAULT_BUFFER_SIZE = 64 * 1024;
    static final int MINIMUM_BUFFER_SIZE = 1024;
    static final int MAXIMUM_BUFFER_SIZE = 32 * 1024 * 1024;

    private final ByteChannel channel;
    private final HttpHeaders requestHeaders = new HttpHeaders();
    private final HttpHeaders responseHeaders = new HttpHeaders();
    private final InputStream inStream;
    private final ByteBuffer bodyBuffer;
    private final Logger logger;

    private boolean suppressHeaders = false;
    private boolean closeOutputIfNoContentLength = false;
    private boolean headersParsed = false;
    private boolean headersWritten = false;
    
    private boolean isChunked() {
        String te = getRequestHeader("Transfer-Encoding");
        return te == null ? false : te.equalsIgnoreCase("chunked");
    }
    
    private boolean isKeepAlive() {
        String ka = getRequestHeader("Connection");
        return ka == null ? false : ka.equalsIgnoreCase("keep-alive");
    }


    public static boolean isUseHTTP() {
        return useHTTP;
    }
    
    // TODO: Add more logging calls to this class

    // --------------------------------------------------------------

    public HttpChannel(ByteChannel channel, String method, String path, int bufferSize, int timeoutMillis, Logger logger) {
        this.channel = channel;
        this.logger = (logger == null) ? Logger.getLogger(getClass().getName()) : logger;

        requestHeaders.setRequestValues(method, path, useHTTP?"HTTP/1.1":"XDBC/1.0");

        if (logger == null) {
            logger = Logger.getLogger(getClass().getName());
        }

        logger.fine("XDBC request: " + requestHeaders.getRequestLine());

        bodyBuffer = allocBuffer(bufferSize);

        inStream = (new ChannelInputStream(channel, bodyBuffer, timeoutMillis));
    }

    public void reset(String method, String path) {
        suppressHeaders = false;
        closeOutputIfNoContentLength = false;
        headersParsed = false;
        headersWritten = false;
        requestHeaders.clear();
        responseHeaders.clear();
        bodyBuffer.clear();

        requestHeaders.setRequestValues(method, path, useHTTP?"HTTP/1.1":"XDBC/1.0");
    }

    // --------------------------------------------------------------

    public ByteChannel getChannel() {
        return channel;
    }

    public void setCloseOutputIfNoContentLength(boolean value) {
        this.closeOutputIfNoContentLength = value;
    }

    // --------------------------------------------------------------

    public int write(byte[] bytes, int offset, int length) throws IOException {
        int srcRemaining = length;

        while (srcRemaining > 0) {
            if (bodyBuffer.remaining() == 0) {
                flushRequest(false);
            }

            int len = Math.min(srcRemaining, bodyBuffer.remaining());

            bodyBuffer.put(bytes, offset + (length - srcRemaining), len);

            srcRemaining -= len;
        }

        return (length);
    }

    public int write(byte[] bytes) throws IOException {
        return (write(bytes, 0, bytes.length));
    }

    public void writeString(String value) throws IOException {
        write(value.getBytes("UTF-8"));
    }

    public void write(ByteBuffer buffer) throws IOException {
        if (buffer.limit() < bodyBuffer.remaining()) {
            write(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining());

            return;
        }

        flushRequest(false);

        writeBuffer(channel, buffer);
    }

    // --------------------------------------------------------------

    public InputStream getResponseStream() throws IOException {
        receiveMode();

        if (getResponseContentLength() != -1) {
            return new LengthLimitedInputStream(inStream, getResponseContentLength());
        }

        return (inStream);
    }

    // --------------------------------------------------------------

    public void setRequestHeader(String header, String value) {
        requestHeaders.setHeader(header, value);
    }

    public String getRequestHeader(String header) {
        return (requestHeaders.getHeader(header));
    }

    // --------------------------------------------------------------

    public void setRequestContentType(String value) {
        requestHeaders.setHeader("Content-Type", value);
    }

    public void setRequestContentLength(int length) {
        requestHeaders.setHeader("Content-Length", "" + length);
    }

    // --------------------------------------------------------------

    public String getResponseHeader(String headerName) throws IOException {
        receiveMode();

        return (responseHeaders.getHeaderNormalized(headerName));
    }

    public int getResponseCode() throws IOException {
        receiveMode();

        return responseHeaders.getResponseCode();
    }

    public String getResponseMessage() throws IOException {
        receiveMode();

        return responseHeaders.getResponseMessage();
    }

    public int getResponseContentLength() throws IOException {
        receiveMode();

        return responseHeaders.getContentLength();
    }

    public String getResponseContentType() throws IOException {
        receiveMode();

        return responseHeaders.getContentType();
    }

    public String getResponseContentTypeField(String fieldName) throws IOException {
        receiveMode();

        return responseHeaders.getContentTypeField(fieldName);
    }

    public String getResponseContentBoundary() throws IOException {
        getResponseContentType(); // insure headers are parsed

        return (responseHeaders.getHeaderSubValue("content-type", "boundary", ";"));
    }

    public String getReponseCookieValue(String key) throws IOException {
        receiveMode();
        return responseHeaders.getHeaderSubValue("set-cookie", key, ";");
    }

    public long getResponseHeaderRecvTime() throws IOException {
        receiveMode();

        // check normalized first, unit testing hack.  Real header is mixed case.
        String val = responseHeaders.getHeaderNormalized(RCV_TIME_HEADER);

        if (val == null) {
            val = responseHeaders.getHeader(RCV_TIME_HEADER);
        }

        if (val == null) {
            return 0;
        }

        return Long.parseLong(val);
    }

    public long getResponseKeepaliveExpireTime() throws IOException {
        receiveMode();

        int keepAliveSeconds = getResponseKeepaliveSeconds();

        if (keepAliveSeconds == 0) {
            return 0;
        }

        return getResponseHeaderRecvTime() + (keepAliveSeconds * 1000);
    }

    public int getResponseKeepaliveSeconds() throws IOException {
        receiveMode();

        String header = responseHeaders.getHeader("connection");

        if ((header == null) || (!header.equalsIgnoreCase("keep-alive"))) {
//			log.debug ("'Connection: Keep-Alive' header not seen");
            return (0);
        }

        Integer val = responseHeaders.getHeaderSubValueInt("keep-alive", "timeout", ",");

        return ((val == null) ? 0 : val.intValue());
    }

    // --------------------------------------------------------------

    public void suppressHeaders() {
        this.suppressHeaders = true;
    }

    // --------------------------------------------------------------

    private void receiveMode() throws IOException {
        flushRequest(true);

        checkCloseOutput();

        if (headersParsed) {
            return;
        }

        parseHeaders();
    }

    // If buffer fills up, content-length can't be computed, so we must issue a close
    // on the output side of the socket so the server will see the end of input.
    // Don't always do this, some code handles keep-alives separately.
    private void checkCloseOutput() throws IOException {
        if (!closeOutputIfNoContentLength) {
            return;
        }

        String connHeader = getRequestHeader("Connection"); // careful: case-sensitive

        if ((connHeader == null) || (!connHeader.equalsIgnoreCase("keep-alive"))) {
            if (channel instanceof SocketChannel) {
                SocketChannel sockChannel = (SocketChannel)channel;

                sockChannel.socket().shutdownOutput();
            }
        }
    }

    private void parseHeaders() throws IOException {
        long now = System.currentTimeMillis();

        logger.finer("parsing response headers");

        responseHeaders.parseResponseHeaders(inStream);

        // conditional for unit testing, never sent by the server
        if (responseHeaders.getHeader(RCV_TIME_HEADER) == null) {
            responseHeaders.setHeader(RCV_TIME_HEADER, "" + now);
        }

        headersParsed = true;
    }

    public String getServerVersion() throws IOException {
        receiveMode();
        String header = getResponseHeader("server");
        if ((header != null) && (header.startsWith("MarkLogic "))) {
        	return header.substring(10);
    	}
        /*
        String responseLine = responseHeaders.getResponseLine();
        if ((responseLine != null) && responseLine.startsWith("XDBC/")) {
            return responseLine.substring(5).split(" ")[0];
        }
        */
        return null;
    }
    
    // --------------------------------------------------------------

    private void flushRequest(boolean finished) throws IOException {
        if (!headersWritten) {
            if (finished) {
                if (!isChunked()) {
                    setRequestContentLength(bodyBuffer.position());
                }
                if (!isKeepAlive()) {
                    setRequestHeader("Connection", "keep-alive");
                }
            }

            writeHeaders();
        }

        writeBody();
    }

    private void writeBody() throws IOException {
        bodyBuffer.flip();

        writeBuffer(channel, bodyBuffer);

        bodyBuffer.clear();
    }

    private void writeHeaders() throws IOException {
        if (!suppressHeaders) {
            byte[] headerBytes = requestHeaders.toString().getBytes("UTF-8");
            ByteBuffer headersBuffer = ByteBuffer.wrap(headerBytes);

            writeBuffer(channel, headersBuffer);
        }

        headersWritten = true;
    }

    private void writeBuffer(ByteChannel channel, ByteBuffer buffer) throws IOException {
        while (buffer.hasRemaining()) {
            channel.write(buffer);
        }
    }

    // --------------------------------------------------------------

    // On some JVMs, specifically IBM's, direct buffers are not GC'ed
    // properly.  If allocation of a direct buffer fails, try a regular one.
    ByteBuffer allocBuffer(int size) {
        int bufSize = (size <= 0) ? DEFAULT_BUFFER_SIZE : size;

        bufSize = Math.max(bufSize, MINIMUM_BUFFER_SIZE);
        bufSize = Math.min(bufSize, MAXIMUM_BUFFER_SIZE);

        try {
            return ByteBuffer.allocateDirect(bufSize);
        } catch (OutOfMemoryError e) {
            return ByteBuffer.allocate(bufSize);
        }
    }

    // --------------------------------------------------------------

    private static class ChannelInputStream extends InputStream {
        private static final int DIRECT_READ_THRESHOLD = 8 * 1024;
        private final ReadableByteChannel channel;
        private final ByteBuffer buffer;
        private int timeoutMillis;
        private Selector selector = null;

        public ChannelInputStream(ReadableByteChannel channel, ByteBuffer buffer, int timeoutMillis) {
            this.channel = channel;
            this.buffer = buffer.duplicate();
            this.timeoutMillis = timeoutMillis;

            this.buffer.clear();
            this.buffer.flip();
        }

        @Override
        public int read(byte bytes[], int off, int len) throws IOException {
            if (len == 0)
                return 0;

            if ((off < 0) || (off > bytes.length) || (len < 0) || ((off + len) > bytes.length) || ((off + len) < 0)) {
                throw new IndexOutOfBoundsException();
            }

            int rc = attemptCopyOut(bytes, off, len);

            if (rc != 0)
                return rc;

            if (len >= DIRECT_READ_THRESHOLD) {
                ByteBuffer buffer = ByteBuffer.wrap(bytes, off, len);

                buffer.position(off);
                buffer.limit(Math.min(off + len, buffer.capacity()));

                return (channel.read(buffer));
            }

            rc = fillBuffer();

            if (rc < 0)
                return -1;

            return attemptCopyOut(bytes, off, len);
        }

        @Override
        public int read(byte b[]) throws IOException {
            return read(b, 0, b.length);
        }

        @Override
        public int read() throws IOException {
            if (buffer.hasRemaining()) {
                return (buffer.get() & 0xff);
            }

            byte[] buf = new byte[1];
            int rc = read(buf, 0, 1);

            if (rc == -1)
                return -1;

            return buf[0] & 255;
        }

        private int attemptCopyOut(byte[] bytes, int off, int len) {
            int bufferedCount = buffer.remaining();
            int toRead = (bufferedCount < len) ? bufferedCount : len;

            if (toRead != 0) {
                buffer.get(bytes, off, toRead);
            }

            return toRead;
        }

        private int fillBuffer() throws IOException {
            buffer.clear();
            int rc = timedRead(buffer);
            buffer.flip();

            return rc;
        }

        private int timedRead(ByteBuffer buffer) throws IOException {
            if (channel instanceof SslByteChannel) {
                SslByteChannel ch = (SslByteChannel)channel;
                int tmp = ch.getTimeout();
                ch.setTimeout(timeoutMillis);
                try {
                    return ch.read(buffer);
                } finally {
                    ch.setTimeout(tmp);
                }
            }

            if ((timeoutMillis <= 0) || (!(channel instanceof SelectableChannel))) {
                return channel.read(buffer);
            }

            SelectableChannel schannel = (SelectableChannel)channel;

            synchronized (channel) {
                SelectionKey key = null;

                if (selector == null) {
                    selector = Selector.open();
                }

                try {
                    selector.selectNow(); // Needed to clear old key state
                    schannel.configureBlocking(false);
                    key = schannel.register(selector, SelectionKey.OP_READ);

                    selector.select(timeoutMillis);

                    int rc = channel.read(buffer);

                    if (rc == 0) {
                        throw new IOException("Timeout waiting for read (" + timeoutMillis + " milliseconds)");
                    }

                    return rc;
                } finally {
                    if (key != null)
                        key.cancel();
                    schannel.configureBlocking(true);
                }
            }
        }
    }
}
