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
package com.marklogic.xcc.impl.handlers;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.marklogic.http.HttpChannel;
import com.marklogic.io.IOHelper;
import com.marklogic.xcc.Content;
import com.marklogic.xcc.ContentCreateOptions;
import com.marklogic.xcc.ContentFactory;
import com.marklogic.xcc.ContentPermission;
import com.marklogic.xcc.DocumentFormat;
import com.marklogic.xcc.DocumentRepairLevel;
import com.marklogic.xcc.Request;
import com.marklogic.xcc.RequestOptions;
import com.marklogic.xcc.ResultSequence;
import com.marklogic.xcc.Session.TransactionMode;
import com.marklogic.xcc.exceptions.ContentInsertException;
import com.marklogic.xcc.exceptions.RequestException;
import com.marklogic.xcc.exceptions.RequestServerException;
import com.marklogic.xcc.impl.RequestImpl;
import com.marklogic.xcc.impl.SessionImpl;
import com.marklogic.xcc.spi.ServerConnection;

public class ContentInsertController extends AbstractRequestController {
    public static final int HTTP_TEMPORARY_REDIRECT = 307;
    static final int DEFAULT_BUFFER_SIZE = 128 * 1024;
    static final int MAX_BUFFER_SIZE = 12 * 1024 * 1024;
    private static final int DATA_CHUNK = 0;
    private static final int COMMIT = 1;
    private static final int NO_COMMIT = 2;

    private static final Map<Integer, ResponseHandler> handlers = new HashMap<Integer, ResponseHandler>(8);

    static {
        addDefaultHandler(handlers, new UnrecognizedCodeHandler());
        addHandler(handlers, HttpURLConnection.HTTP_UNAVAILABLE, new ServiceUnavailableHandler());
        addHandler(handlers, HttpURLConnection.HTTP_INTERNAL_ERROR, new ServerExceptionHandler());
        addHandler(handlers, HttpURLConnection.HTTP_UNAUTHORIZED, new UnauthorizedHandler());
        addHandler(handlers, HttpURLConnection.HTTP_NOT_FOUND, new NotFoundCodeHandler());
        addHandler(handlers, HttpURLConnection.HTTP_BAD_REQUEST, new NotFoundCodeHandler());
        addHandler(handlers, HttpURLConnection.HTTP_OK, new GoodInsertResponseHandler());
        addHandler(handlers, HTTP_TEMPORARY_REDIRECT, new EntityResolveHandler());
        addHandler(handlers, HttpURLConnection.HTTP_UNSUPPORTED_TYPE, new UnSupportedTypeHandler());
        addHandler(handlers, HttpURLConnection.HTTP_BAD_METHOD, new BadMethodHandler());
        addHandler(handlers, HttpURLConnection.HTTP_ENTITY_TOO_LARGE, new EntityTooLargeHandler());
    }

    // --------------------------------------------------------

    private final Content[] contents;
    private final ByteBuffer headerBuffer = ByteBuffer.allocate(16);
    private final LinkedList<Content> processedContent = new LinkedList<Content>();
    private ByteBuffer dataBuffer = null;
    private boolean collectErrors;
    private List<RequestException> errorList;
    
    public ContentInsertController(Content[] contents, 
            TransactionMode txnMode) {
        this(contents, txnMode, false);
    }

    public ContentInsertController(Content[] contents, TransactionMode txnMode,
            boolean ignoreErrors) {
        super(handlers);

        //noinspection RedundantCast
        this.contents = contents.clone();
        this.collectErrors = ignoreErrors;
    }

    // --------------------------------------------------------
    // Invoked by superclass template method

    @Override
    public ResultSequence serverDialog(ServerConnection connection, Request request, RequestOptions options,
            Logger logger) throws RequestException, IOException {
        assertRestartable(processedContent, request);

        LinkedList<ContentDecorator> remaining = toLinkedList(contents);
        
        logger.fine("beginning content insert dialog, " + remaining.size() + " documents queued");

        HttpChannel http = new HttpChannel(connection.channel(), "PUT", "/", 0, options.getTimeoutMillis(), logger);
 
        while (remaining.size() > 0) {
            if (logger.isLoggable(Level.FINE))
                logger.fine("" + processedContent.size() + " items sent, " + remaining.size() + " remaining");

            ContentDecorator content = remaining.remove(0);
            boolean commit = remaining.size() == 0;

            if (logger.isLoggable(Level.FINE)) {
                if (content.isEntity()) {
                    logger.fine("processing entity '" + content.getLocation() + "' for document '" + content.getUri()
                            + "'");
                } else {
                    logger.fine("processing '" + content.getUri() + "'");
                }
            }
            resetHttpChannel(http, request, options, content, commit, logger);

            issueRequest(http, content, commit, logger);

            int code = http.getResponseCode();
            
            SessionImpl session = (SessionImpl)request.getSession();
            session.setServerVersion(http.getServerVersion());
            
            if (!session.readCookieValues(http)) {
                String version = session.getServerVersion();
                throw new RequestServerException("Incompatible server version " 
                    + version == null ? "" : version + 
                    ".  Make sure to set xcc.txn.compatible to true", request);
            }
            
            ContentDecorator entityContent = null;
            
            try {
                ResponseHandler handler = findHandler(code);
                entityContent = (ContentDecorator)handler.handleResponse(http, code, request, content, logger);
            } catch (RequestServerException e) {
                if (collectErrors) {
                    if (errorList == null) {
                        errorList = new ArrayList<RequestException>();
                    }
                    errorList.add(new ContentInsertException(e.getMessage(), 
                            e.getRequest(), content.content, e));
                } else {
                    throw e;
                }
            } finally {
                if (connection.isOpen()) {
                    setConnectionTimeout(connection, http);
                }
            }

            if (!content.isEntity()) {
                processedContent.add(content);
            }
            if (entityContent != null) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("queueing entity content for '" + entityContent.getUri() + "', location: "
                            + entityContent.getLocation());
                }

                remaining.addFirst(entityContent);
            }
        }

        closeContent(processedContent);

        logger.fine("finished content insert dialog, " + contents.length + " documents successfully inserted");

        return null;
    }

    // --------------------------------------------------------

    private static final int PLATEAU = 2000;
    private static final int PLATEAU_SHORT_CIRCUIT = 20;

    @Override
    protected long interTryDelay(long delay, int currentTry) {
        if ((currentTry == 0) || (delay <= 0)) {
            return 0;
        }

        if (currentTry >= PLATEAU_SHORT_CIRCUIT)
            return PLATEAU;

        long millis = delay * (1 << (currentTry - 1));

        // FIXME: Need to add API methods to ContentCreateOptions to customize this
        return (millis > PLATEAU) ? PLATEAU : millis;
    }

    // --------------------------------------------------------

    private void resetHttpChannel(HttpChannel http, Request request, RequestOptions options, Content content,
            boolean commit, Logger logger) {
        SessionImpl session = (SessionImpl)request.getSession();
        String pathUri = (content.getUri() == null) ? null : 
            makeReqUri(content, request, commit, collectErrors);

        String method = "PUT";
        http.reset(method, pathUri);

        ContentCreateOptions copt = content.getCreateOptions();
        if (copt != null
            && !ContentCreateOptions.DEFAULT_ENCODING.equalsIgnoreCase(copt.getEncoding())) {
            http.setRequestContentType("text/xml; charset="+copt.getEncoding());
        } else {
            http.setRequestContentType("text/xml");
        }
        addCommonHeaders(http, session, method, pathUri, options, logger);
        http.setRequestHeader("Connection", "keep-alive");
        if(HttpChannel.isUseHTTP()) {
            http.setRequestHeader("Transfer-Encoding", "chunked");
        }
    }

    // --------------------------------------------------------

    private void issueRequest(HttpChannel http, ContentDecorator content, boolean commit, Logger logger)
            throws IOException {
        String uri = content.getUri();

        if (logger.isLoggable(Level.FINE)) {
            if (content.isEntity()) {
                logger.fine("sending entity (location=" + content.getLocation() + ") for uri=" + uri + ", size="
                        + content.size());
            } else {
                logger.fine("sending content: uri=" + uri + ", size=" + content.size());
            }
        }

        ByteBuffer dataBuffer = allocDataBuffer(content);
        byte[] dataBytes = dataBuffer.array();
        InputStream inStream = content.openDataStream();
        boolean checkBOM = mayHaveBOM(content);
        int rc;

        if (content.isEntity()) {
            http.suppressHeaders();
        }

        while ((rc = inStream.read(dataBytes)) > 0) {
            dataBuffer.clear();
            dataBuffer.limit(rc);

            if (checkBOM) {
                checkBOM = false;

                if ((rc >= 3) && hasBOM(dataBytes)) {
                    rc -= 3;
                    dataBuffer.position(3);
                    logger.finest("suppressed UTF-8 BOM");
                }
            }

            writeChunkHeader(http, DATA_CHUNK, rc, logger);

            if (logger.isLoggable(Level.FINEST))
                logger.finest("writing " + rc + " bytes of data");

            http.write(dataBuffer); 
            if (HttpChannel.isUseHTTP()) {
                http.write("\r\n".getBytes());
            }
        }

        inStream.close();

        writeChunkHeader(http, (commit) ? COMMIT : NO_COMMIT, 0, logger);

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("finished sending content: commit=" + commit);
        }
    }

    private boolean mayHaveBOM(ContentDecorator content) {
        ContentCreateOptions options = content.getCreateOptions();
        String encoding = (options == null) ? "utf-8" : options.getEncoding();
        DocumentFormat fmt = (options == null) ? DocumentFormat.NONE : options.getFormat();
        boolean isUtf8 = (options == null) || ("utf-8".equalsIgnoreCase(encoding) || "utf8".equalsIgnoreCase(encoding));

        return isUtf8 && (fmt != DocumentFormat.BINARY) && (fmt != DocumentFormat.NONE);
    }

    private boolean hasBOM(byte[] bytes) {
        // UTF-8 encoded Byte-Order-Mark is efbbbf as the first three bytes
        return ((bytes[0] & 0xff) == 0xef) && ((bytes[1] & 0xff) == 0xbb) && ((bytes[2] & 0xff) == 0xbf);
    }

    // ------------------------------------------------------------

    private void assertRestartable(LinkedList<Content> processedContent, Request request) throws ContentInsertException {
        while (processedContent.size() > 0) {
            ContentDecorator content = (ContentDecorator)processedContent.removeFirst();

            if (content.isPristine()) {
                continue;
            }

            if (content.isRewindable()) {
                try {
                    // TODO: write a test case to verify rewinds
                    content.rewind();
                } catch (IOException e) {

                    processedContent.clear();

                    // TODO: write a test case for this
                    throw new ContentInsertException("Cannot auto-restart insert, error rewinding content: "
                            + content.getUri(), request, content.getOriginal(), e);
                }

                continue;
            }

            processedContent.clear();

            throw new ContentInsertException("Cannot auto-restart insert, non-rewindable content already processed: "
                    + content.getUri(), request, content.getOriginal());
        }
    }

    private void closeContent(LinkedList<Content> processedContent) {
        while (processedContent.size() > 0) {
            Content content = processedContent.removeFirst();

            content.close();
        }
    }

    // ------------------------------------------------------------

    private LinkedList<ContentDecorator> toLinkedList(Content[] array) {
        LinkedList<ContentDecorator> list = new LinkedList<ContentDecorator>();

        if (array.length > 0) {
            Content first = array[0];
            if (!first.isRewindable()) {
                list.add(new ContentDecorator(
                        ContentFactory.newContent(
                                first.getUri(), 
                                new byte[0], 
                                first.getCreateOptions())));
            }
        }

        for (int i = 0; i < array.length; i++) {
            list.add(new ContentDecorator(array[i]));
        }

        return (list);
    }

    /*
     * http 1.1 chunk encoding
     * refer to rfc2616 at http://tools.ietf.org/html/rfc2616#section-3.6.1
     */
    private void writeChunkHeader(HttpChannel http, int code, int count, Logger logger) throws IOException {
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest("writing chunk header: " + code + count);
        }

        headerBuffer.clear();
        if (!HttpChannel.isUseHTTP()) {
            headerBuffer.put((byte)('0' + code));
            headerBuffer.put(Integer.toString(count).getBytes());
            headerBuffer.put((byte)'\r');
            headerBuffer.put((byte)'\n');
        } else {
            headerBuffer.put(Integer.toHexString(count).getBytes());
            headerBuffer.put((byte)'\r');
            headerBuffer.put((byte)'\n');
            if (count == 0) {
                //an empty line follows the last chunk
                headerBuffer.put((byte)'\r');
                headerBuffer.put((byte)'\n');
            } 
        }
        
        headerBuffer.flip();

        http.write(headerBuffer);
    }

    // package visible for unit testing
    ByteBuffer allocDataBuffer(Content content) {
        ContentCreateOptions options = content.getCreateOptions();
        int userSize = ((options == null) ? -1 : options.getBufferSize());
        long bufSize = ((content.size() == -1) ? DEFAULT_BUFFER_SIZE : content.size());

        bufSize = (userSize == -1) ? bufSize : Math.min(bufSize, userSize);
        bufSize = Math.min(bufSize, MAX_BUFFER_SIZE);

        if ((dataBuffer == null) || (dataBuffer.capacity() < bufSize)) {
            dataBuffer = ByteBuffer.allocate((int)bufSize);//safe to convert since MAX_BUFFER_SIZE is smaller than 2G
        }

        return dataBuffer;
    }

    // package visible for unit testing
    static String makeReqUri(Content content, Request request, boolean commit,
            boolean collectErrors) {
        ContentCreateOptions options = (content.getCreateOptions() == null) ? new ContentCreateOptions() : content
                .getCreateOptions();
        RequestOptions requestOptions = (request == null) ? new RequestOptions() : request.getEffectiveOptions();
        StringBuffer sb = new StringBuffer(256);

        sb.append("/insert?uri=");
        IOHelper.urlEncodeToStringBuffer(sb, content.getUri());

        if (!commit) {
            sb.append("&nocommit");
        }
        
        if (collectErrors) {
            sb.append("&errok");
        }

        if (options.getLocale() != null) {
            if (request == null) {
                sb.append("&locale=").append(options.getLocale().toString());
            } else {
                requestOptions.setLocale(options.getLocale());
            }
        }

        if (options.getLanguage() != null) {
            sb.append("&lang=");
            IOHelper.urlEncodeToStringBuffer(sb, options.getLanguage());
        }

        if (options.getNamespace() != null) {
            sb.append("&defaultns=");
            IOHelper.urlEncodeToStringBuffer(sb, options.getNamespace());
        }

        if (options.getQuality() != 0) {
            sb.append("&quality=").append(options.getQuality());
        }

        if (options.getResolveEntities()) {
            sb.append("&resolve");
        }

        if (options.getResolveBufferSize() != 0) {
            sb.append("&resolvesiz=").append(options.getResolveBufferSize());
        }

        if (options.getRepairLevel() == DocumentRepairLevel.NONE) {
            sb.append("&repair=none");
        }

        if (options.getRepairLevel() == DocumentRepairLevel.FULL) {
            sb.append("&repair=full");
        }

        if (options.getFormat() == DocumentFormat.XML) {
            sb.append("&format=xml");
        }
        
        if (options.getFormat() == DocumentFormat.JSON) {
            sb.append("&format=json");
        }

        if (options.getFormat() == DocumentFormat.TEXT) {
            sb.append("&format=text");
        }

        if (options.getFormat() == DocumentFormat.BINARY) {
            sb.append("&format=binary");
        }

        if (options.getPlaceKeys() != null) {
            BigInteger[] keys = options.getPlaceKeys();

            for (int i = 0; i < keys.length; i++) {
                sb.append("&placeKey=").append(keys[i].toString());
            }
        }

        if (options.getCollections() != null) {
            String[] collections = options.getCollections();

            if (collections.length == 0) {
                sb.append("&nocolls");
            } else {
                for (int i = 0; i < collections.length; i++) {
                    sb.append("&coll=");
                    IOHelper.urlEncodeToStringBuffer(sb, collections[i]);
                }
            }
        }

        if (options.getPermissions() != null) {
            ContentPermission[] perms = options.getPermissions();

            if (perms.length == 0) {
                sb.append("&noperms");
            } else {
                for (int i = 0; i < perms.length; i++) {
                    ContentPermission perm = perms[i];
                    String symbol = (perm.getCapability() == null) ? "N" : perm.getCapability().getSymbol();

                    sb.append("&perm=").append(symbol);
                    IOHelper.urlEncodeToStringBuffer(sb, perm.getRole());
                }
            }
        }

        if (options.getTemporalCollection() != null) {
            sb.append("&temporalcoll=");
            IOHelper.urlEncodeToStringBuffer(sb, options.getTemporalCollection());
        }
        
        if (options.getGraph() != null) {
            sb.append("&graph=");
            IOHelper.urlEncodeToStringBuffer(sb, options.getGraph());
        }

        if (request != null) {
            ((RequestImpl)request).encodeQueryOptions(sb, requestOptions);
            ((RequestImpl)request).encodeTxn(sb);
        }
        
       
        return sb.substring(0);
    }

    // ------------------------------------------------------------

    static class ContentDecorator implements Content {
        final Content content;
        private final Content parent;
        private final String location;
        private boolean pristine = true;

        public ContentDecorator(Content content) {
            this.content = content;
            this.parent = null;
            this.location = null;
        }

        public ContentDecorator(Content entity, Content parent, String location) {
            this.content = entity;
            this.parent = parent;
            this.location = location;
        }

        // ----------------------------------------------------
        // Content interface

        public String getUri() {
            return (parent == null) ? content.getUri() : parent.getUri();
        }

        public InputStream openDataStream() throws IOException {
            pristine = false;

            return content.openDataStream();
        }

        public ContentCreateOptions getCreateOptions() {
            return content.getCreateOptions();
        }

        public boolean isRewindable() {
            return content.isRewindable();
        }

        public void rewind() throws IOException {
            pristine = false;

            content.rewind();
        }

        public long size() {
            return (content.size());
        }

        public void close() {
            pristine = false;

            content.close();
        }

        // -----------------------------------------------------
        // decorations

        public boolean isEntity() {
            return location != null;
        }

        public String getLocation() {
            return location;
        }

        public boolean isPristine() {
            return pristine;
        }

        public Content getOriginal() {
            return content;
        }
    }

    public List<RequestException> getErrors() {
        return errorList;
    }
}

/*
 * [CLIENT]: -->PUT
 * /insert?uri=%2Ftestdocs%2Ffooinsertentity.xml&resolve&resolvesiz=1048576&format=xml HTTP/1.1
 * Content-Type: text/xml User-Agent: Java/1.4.2_09 MarkXDBC/3.0-5 Host: localhost:9050 Accept:
 * text/html, text/xml, image/gif, image/jpeg, * /* Connection: keep-alive Authorization: basic
 * eGRiYzp4ZGJj
 * 
 * 0159 <!DOCTYPE root PUBLIC "http://marklogic.com/foo/blah" [<!ENTITY myentity SYSTEM
 * "file:///tmp/junittmpentity"> ]> <root><blah foo="boo">&myentity;</blah></root>10 <--
 * 
 * [SERVER]: -->HTTP/1.1 307 Temporary Redirect Location: file:/tmp/junittmpentity Content-Length: 0
 * 
 * <--
 * 
 * [CLIENT]: -->013 <--
 * 
 * [CLIENT]: -->1139642485452<--
 * 
 * [CLIENT]: -->10 <--
 * 
 * [SERVER]: -->HTTP/1.1 200 OK Content-Length: 0 Connection: Keep-Alive Keep-Alive: timeout=1
 * 
 * <--
 */

