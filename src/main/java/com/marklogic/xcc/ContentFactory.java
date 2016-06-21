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
package com.marklogic.xcc;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.nio.channels.Channels;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.fasterxml.jackson.databind.JsonNode;
import com.marklogic.xcc.types.XdmNode;

// Need an example of a user-implementation of the {@link Content}
// interface for sources like Web Services or SQL ResultSets.
/**
 * <p>
 * A helper class for creating instances of {@link Content}.
 * </p>
 */
public class ContentFactory {
    private ContentFactory() {
        // This is a helper class, cannot be instantiated
    }

    /**
     * Create a new {@link Content} object from a W3C DOM {@link Document} object. If not explicitly
     * overridden, the document format will be set to XML. This factory method makes use of the
     * standard JAX {@link TransformerFactory} facility to serialize the DOM to a UTF-8-encoded byte
     * stream. See {@link javax.xml.transform.TransformerFactory#newInstance()} for information on
     * how to customize the transformer implementation.
     * 
     * @param uri
     *            The URI (name) with which the document will be inserted into the content store. If
     *            the URI already exists in the store, it will be replaced with the new content.
     * @param document
     *            A W3C DOM {@link Document} object which is the content.
     * @param createOptions
     *            Creation meta-information to be applied when the content is inserted into the
     *            contentbase. These options control the document format (json, xml, text, binary) and
     *            access permissions.
     * @return A {@link Content} object suitable for passing to
     *         {@link Session#insertContent(Content)}
     */
    public static Content newContent(String uri, Document document, ContentCreateOptions createOptions) {
        return newContent(uri, bytesFromW3cDoc(document), (createOptions == null) ? ContentCreateOptions
                .newXmlInstance() : createOptions);
    }

    /**
     * Create a new {@link Content} object from a W3C DOM {@link Node} object.
     * 
     * @param uri
     *            The URI (name) with which the document will be inserted into the content store. If
     *            the URI already exists in the store, it will be replaced with the new content. If
     *            not explicitly overridden, the document format will be set to XML. This factory
     *            method makes use of the standard JAX {@link TransformerFactory} facility to
     *            serialize the DOM to a UTF-8-encoded byte stream. See
     *            {@link javax.xml.transform.TransformerFactory#newInstance()} for information on
     *            how to customize the transformer implementation.
     * @param documentNode
     *            A W3C DOM {@link Node} object which is the content. Only Nodes of type Element or
     *            Text are valid.
     * @param createOptions
     *            Creation meta-information to be applied when the content is inserted into the
     *            contentbase. These options control the document format (json, xml, text, binary) and
     *            access permissions.
     * @return A {@link Content} object suitable for passing to
     *         {@link Session#insertContent(Content)}
     */
    public static Content newContent(String uri, Node documentNode, ContentCreateOptions createOptions) {
        return newContent(uri, bytesFromW3cNode(documentNode), (createOptions == null) ? ContentCreateOptions
                .newXmlInstance() : createOptions);
    }
    
    /**
     * Create a new {@link Content} object from a Jackson {@link Node} object.
     * 
     * @param uri
     *            The URI (name) with which the document will be inserted into the content store. If
     *            the URI already exists in the store, it will be replaced with the new content. If
     *            not explicitly overridden, the document format will be set to JSON.
     * @param documentNode
     *            A Jackson {@link Node} object which is the content. Only Nodes of type Array or
     *            Object are valid.
     * @param createOptions
     *            Creation meta-information to be applied when the content is inserted into the
     *            contentbase. These options control the document format (json, xml, text, binary) and
     *            access permissions.
     * @return A {@link Content} object suitable for passing to
     *         {@link Session#insertContent(Content)}
     */
    public static Content newJsonContent(String uri, JsonNode documentNode, ContentCreateOptions createOptions) {
        return newContent(uri, bytesFromString(documentNode.toString()), (createOptions == null) ? ContentCreateOptions
                .newJsonInstance() : createOptions);
    }

    /**
     * Create a new {@link Content} object from an {@link XdmNode}. This object may have been
     * constructed locally or received as a member of a {@link ResultSequence}. If not explicitly
     * overridden, the document format will be set to XML.
     * 
     * @param uri
     *            The URI (name) with which the document will be inserted into the content store. If
     *            the URI already exists in the store, it will be replaced with the new content.
     * @param documentNode
     *            An instance of {@link XdmNode} which is the document content. Only nodes of type
     *            {@link com.marklogic.xcc.types.XdmElement},
     *            {@link com.marklogic.xcc.types.XdmText} or
     *            {@link com.marklogic.xcc.types.XdmBinary} are valid.
     * @param createOptions
     *            Creation meta-information to be applied when the content is inserted into the
     *            contentbase. These options control the document format (json, xml, text, binary) and
     *            access permissions.
     * @return A {@link Content} object suitable for passing to
     *         {@link Session#insertContent(Content)}
     */
    public static Content newContent(String uri, XdmNode documentNode, ContentCreateOptions createOptions) {
        return newContent(uri, bytesFromString(documentNode.asString()), (createOptions == null) ? ContentCreateOptions
                .newXmlInstance() : createOptions);
    }

    /**
     * <p>
     * Create a new {@link Content} object from a File object.
     * </p>
     * 
     * @param uri
     *            The URI (name) with which the document will be inserted into the content store. If
     *            the URI already exists in the store, it will be replaced with the new content.
     * @param documentFile
     *            A File object from which the content will be read. If the createOptions argument
     *            selects {@link DocumentFormat#BINARY}, the content of the file will be transfered
     *            as an opaque blob. Otherwise, the file is assumed to be UTF-8 encoded text.
     * @param createOptions
     *            Creation meta-information to be applied when the content is inserted into the
     *            contentbase. These options control the document format (json, xml, text, binary) and
     *            access permissions.
     * @return A {@link Content} object suitable for passing to
     *         {@link Session#insertContent(Content)}
     */
    public static Content newContent(String uri, File documentFile, ContentCreateOptions createOptions) {
        return new FileContent(uri, documentFile, createOptions);
    }

    /**
     * Create a new {@link Content} object from a RandomAccessFile object.
     * 
     * @param uri
     *            The URI (name) with which the document will be inserted into the content store. If
     *            the URI already exists in the store, it will be replaced with the new content.
     * @param documentFile
     *            An open RandomAccessaFile object from which the content will be read. File data
     *            will be read from the current position to end-of-file. If the createOptions
     *            argument selects {@link DocumentFormat#BINARY}, the content of the file will be
     *            transfered as an opaque blob. Otherwise, the file is assumed to be UTF-8 encoded
     *            text.
     * @param createOptions
     *            Creation meta-information to be applied when the content is inserted into the
     *            contentbase. These options control the document format (json, xml, text, binary) and
     *            access permissions.
     * @return A {@link Content} object suitable for passing to
     *         {@link Session#insertContent(Content)}
     * @throws java.io.IOException
     *             If there is a problem reading data from the file.
     */
    public static Content newContent(String uri, RandomAccessFile documentFile, ContentCreateOptions createOptions)
            throws IOException {
        return new RandomAccessFileContent(uri, documentFile, createOptions);
    }

    /**
     * Create a new, non-rewindable {@link Content} object from a {@link URL}.
     * 
     * @param uri
     *            The URI (name) with which the document will be inserted into the content store. If
     *            the URI already exists in the store, it will be replaced with the new content.
     * @param documentUrl
     *            A {@link URL} object that represents the location from which the content can be
     *            fetched.
     * @param createOptions
     *            Creation meta-information to be applied when the content is inserted into the
     *            contentbase. These options control the document format (json, xml, text, binary) and
     *            access permissions.
     * @return A {@link Content} object suitable for passing to
     *         {@link Session#insertContent(Content)}
     * @throws IOException
     *             If there is a problem creating a {@link URL} or opening a data stream.
     */
    public static Content newUnBufferedContent(String uri, URL documentUrl, ContentCreateOptions createOptions)
            throws IOException {
        return new InputStreamContent(uri, documentUrl.openStream(), createOptions);
    }

    /**
     * Create a new, non-rewindable {@link Content} object from a {@link URI}.
     * 
     * @param uri
     *            The {@link URI} (name) with which the document will be inserted into the content
     *            store. If the URI already exists in the store, it will be replaced with the new
     *            content.
     * @param documentUri
     *            A URI object that represents the location from which the content can be fetched.
     * @param createOptions
     *            Creation meta-information to be applied when the content is inserted into the
     *            contentbase. These options control the document format (json, xml, text, binary) and
     *            access permissions.
     * @return A {@link Content} object suitable for passing to
     *         {@link Session#insertContent(Content)}
     * @throws IOException
     *             If there is a problem creating a {@link URL} or opening a data stream.
     */
    public static Content newUnBufferedContent(String uri, URI documentUri, ContentCreateOptions createOptions)
            throws IOException {
        return newUnBufferedContent(uri, documentUri.toURL(), createOptions);
    }

    /**
     * Create a new {@link Content} object from a URI, buffering so that it's rewindable and only
     * accesses the URL one time.
     * 
     * @param uri
     *            The URI (name) with which the document will be inserted into the content store. If
     *            the URI already exists in the store, it will be replaced with the new content.
     * @param documentUrl
     *            A {@link URL} object that represents the location from which the content can be
     *            fetched.
     * @param createOptions
     *            Creation meta-information to be applied when the content is inserted into the
     *            contentbase. These options control the document format (json, xml, text, binary) and
     *            access permissions.
     * @return A {@link Content} object suitable for passing to
     *         {@link Session#insertContent(Content)}
     * @throws IOException
     *             If there is a problem creating a {@link URL} or reading the data from the stream.
     */
    public static Content newContent(String uri, URL documentUrl, ContentCreateOptions createOptions)
            throws IOException {
        return new ByteArrayContent(uri, bytesFromStream(documentUrl.openStream()), createOptions);
    }

    /**
     * Create a new {@link Content} object from a URI, buffering so that it's rewindable and only
     * accesses the URL one time.
     * 
     * @param uri
     *            The URI (name) with which the document will be inserted into the content store. If
     *            the URI already exists in the store, it will be replaced with the new content.
     * @param documentUri
     *            A {@link URI} object that represents the location from which the content can be
     *            fetched. Unlike {@link URL}s, {@link URI} objects are not validated when they are
     *            created. This method converts the {@link URI} to a {@link URL}. If this parameter
     *            does not represent a valid URL, an exception will be thrown.
     * @param createOptions
     *            Creation meta-information to be applied when the content is inserted into the
     *            contentbase. These options control the document format (json, xml, text, binary) and
     *            access permissions.
     * @return A {@link Content} object suitable for passing to
     *         {@link Session#insertContent(Content)}
     * @throws IOException
     *             If there is a problem creating a {@link URL} or reading the data from the stream.
     * @throws IllegalArgumentException
     *             If the URI is not absolute.
     */
    public static Content newContent(String uri, URI documentUri, ContentCreateOptions createOptions)
            throws IOException {
        return newContent(uri, documentUri.toURL(), createOptions);
    }

    /**
     * Create a new {@link Content} object from a String.
     * 
     * @param uri
     *            The URI (name) with which the document will be inserted into the content store. If
     *            the URI already exists in the store, it will be replaced with the new content.
     * @param documentString
     *            A String which will be stored as the content of the document. If the createOptions
     *            argument selects {@link DocumentFormat#BINARY}, the bytes representing the UTF-8
     *            encoding of the String will be stored.
     * @param createOptions
     *            Creation meta-information to be applied when the content is inserted into the
     *            contentbase. These options control the document format (json, xml, text, binary) and
     *            access permissions.
     * @return A {@link Content} object suitable for passing to
     *         {@link Session#insertContent(Content)}
     * @see DocumentFormat
     */
    public static Content newContent(String uri, String documentString, ContentCreateOptions createOptions) {
        return newContent(uri, bytesFromString(documentString), createOptions);
    }

    /**
     * Create a new {@link Content} object from a byte array.
     * 
     * @param uri
     *            The URI (name) with which the document will be inserted into the content store. If
     *            the URI already exists in the store, it will be replaced with the new content.
     * @param documentBytes
     *            A byte array which will be stored as the content of the document. If the
     *            createOptions argument is null, then {@link DocumentFormat#BINARY} will be
     *            assumed.
     * @param createOptions
     *            Creation meta-information to be applied when the content is inserted into the
     *            contentbase. These options control the document format (json, xml, text, binary) and
     *            access permissions.
     * @return A {@link Content} object suitable for passing to
     *         {@link Session#insertContent(Content)}
     * @see DocumentFormat
     */
    public static Content newContent(String uri, byte[] documentBytes, ContentCreateOptions createOptions) {
        return new ByteArrayContent(uri, documentBytes, createOptions);
    }

    /**
     * Create a new {@link Content} object from a subset of a byte array.
     * 
     * @param uri
     *            The URI (name) with which the document will be inserted into the content store. If
     *            the URI already exists in the store, it will be replaced with the new content.
     * @param documentBytes
     *            A byte array which will be stored as the content of the document. If the
     *            createOptions argument is null, then {@link DocumentFormat#BINARY} will be
     *            assumed.
     * @param createOptions
     *            Creation meta-information to be applied when the content is inserted into the
     *            contentbase. These options control the document format (json, xml, text, binary) and
     *            access permissions.
     * @param offset
     *            The starting point of the content in the array.
     * @param length
     *            The length of the content in the array.
     * @return A {@link Content} object suitable for passing to
     *         {@link Session#insertContent(Content)}
     * @see DocumentFormat
     * @since 3.2
     */
    public static Content newContent(String uri, byte[] documentBytes, int offset, int length,
            ContentCreateOptions createOptions) {
        return new ByteArrayContent(uri, documentBytes, offset, length, createOptions);
    }

    // ---------------------------------------------------------------

    /**
     * Create a new {@link Content} object by consuming the given InputStream and buffereing it in
     * memory. This factory method immediately reads the stream to the end and buffers it. This
     * could result in an OutOfMemoryError if the stream contains a large amount of data. The
     * provided documentStream will be closed.
     * 
     * @param uri
     *            The URI (name) with which the document will be inserted into the content store. If
     *            the URI already exists in the store, it will be replaced with the new value.
     * @param documentStream
     *            The stream making up the document content.
     * @param createOptions
     *            Creation meta-information to be applied when the content is inserted into the
     *            contentbase. These options control the document format (json, xml, text, binary) and
     *            access permissions.
     * @return A {@link Content} object suitable for passing to
     *         {@link Session#insertContent(Content)}
     * @throws IOException
     *             If there is a problem reading the documentStream.
     */
    public static Content newContent(String uri, InputStream documentStream, ContentCreateOptions createOptions)
            throws IOException {
        return new ByteArrayContent(uri, bytesFromStream(documentStream), createOptions);
    }

    /**
     * <p>
     * Create a new non-rewindable {@link Content} object for the given InputStream. Note that the
     * {@link Content} instance returned is not rewindable (
     * {@link com.marklogic.xcc.Content#isRewindable()} == false) which means that auto-retry cannot
     * be performed is there is a problem inserting the content.
     * </p>
     * 
     * @param uri
     *            The URI (name) with which the document will be inserted into the content store. If
     *            the URI already exists in the store, it will be replaced with the new value.
     * @param documentStream
     *            The stream making up the document content.
     * @param createOptions
     *            Creation meta-information to be applied when the content is inserted into the
     *            contentbase. These options control the document format (json, xml, text, binary) and
     *            access permissions.
     * @return A non-rewindable {@link Content} object suitable for passing to
     *         {@link Session#insertContent(Content)}
     */
    public static Content newUnBufferedContent(String uri, InputStream documentStream,
            ContentCreateOptions createOptions) {
        return new InputStreamContent(uri, documentStream, createOptions);
    }

    // ---------------------------------------------------------------

    private static byte[] bytesFromString(String string) {
        try {
            return string.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            return string.getBytes();
        }
    }

    private static byte[] bytesFromStream(InputStream is) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buffer = new byte[64 * 1024];
        int rc;

        while ((rc = is.read(buffer)) != -1) {
            os.write(buffer, 0, rc);
        }

        is.close();
        os.flush();
        buffer = os.toByteArray();
        os.close();

        return (buffer);
    }

    private static TransformerFactory transformerFactory = null;

    private static synchronized TransformerFactory getTransformerFactory() {
        if (transformerFactory == null) {
            transformerFactory = TransformerFactory.newInstance();
        }

        return transformerFactory;
    }

    static byte[] bytesFromW3cNode(Node node) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Result rslt = new StreamResult(bos);
        Source src = new DOMSource(node);
        Transformer transformer;

        try {
            transformer = getTransformerFactory().newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.transform(src, rslt);
        } catch (TransformerException e) {
            throw new RuntimeException("Cannot serialize Node: " + e, e);
        }

        return bos.toByteArray();
    }

    static byte[] bytesFromW3cDoc(Document document) {
        return (bytesFromW3cNode(document.getDocumentElement()));
    }

    // ---------------------------------------------------------------
    // ---------------------------------------------------------------

    private static class ByteArrayContent implements Content {
        private final String uri;
        private final byte[] bytes;
        private final int offset;
        private final int length;
        private final ContentCreateOptions options;

        public ByteArrayContent(String uri, byte[] bytes, int offset, int length, ContentCreateOptions options) {
            if ((uri == null) || (uri.length() == 0)) {
                throw new IllegalArgumentException("URI may not be null or zero-length");
            }
            this.uri = uri;
            this.bytes = bytes;
            this.offset = offset;
            this.length = length;
            this.options = options;
        }

        public ByteArrayContent(String uri, byte[] bytes, ContentCreateOptions options) {
            this(uri, bytes, 0, bytes.length, options);
        }

        public String getUri() {
            return (uri);
        }

        public InputStream openDataStream() {
            return new ByteArrayInputStream(bytes, offset, length);
        }

        public ContentCreateOptions getCreateOptions() {
            return options;
        }

        public boolean isRewindable() {
            return true;
        }

        public void rewind() {
        }

        public long size() {
            return length;
        }

        public void close() {
        }
    }

    private static class FileContent implements Content {
        private final String uri;
        private final ContentCreateOptions options;
        private final File file;
        private InputStream activeStream;

        public FileContent(String uri, File file, ContentCreateOptions options) {
            if ((uri == null) || (uri.length() == 0)) {
                throw new IllegalArgumentException("URI may not be null or zero-length");
            }
            this.uri = uri;
            this.file = file;
            this.options = options;
        }

        public String getUri() {
            return uri;
        }

        public InputStream openDataStream() throws FileNotFoundException {
            activeStream = new BufferedInputStream(new FileInputStream(file));

            return activeStream;
        }

        public ContentCreateOptions getCreateOptions() {
            return options;
        }

        public boolean isRewindable() {
            return true;
        }

        public void rewind() throws IOException {
            close();
        }

        public long size() {
            return file.length();
        }

        public void close() {
            if (activeStream != null) {
                try {
                    activeStream.close();
                } catch (IOException e) {
                    // don't care
                }

                activeStream = null;
            }
        }
    }

    private static class RandomAccessFileContent implements Content {
        private final String uri;
        private final ContentCreateOptions options;
        private final RandomAccessFile raFile;
        private final long start;

        public RandomAccessFileContent(String uri, RandomAccessFile raFile, ContentCreateOptions options)
                throws IOException {
            if ((uri == null) || (uri.length() == 0)) {
                throw new IllegalArgumentException("URI may not be null or zero-length");
            }
            this.uri = uri;
            this.raFile = raFile;
            this.options = options;

            start = raFile.getFilePointer();
        }

        public String getUri() {
            return uri;
        }

        public InputStream openDataStream() throws IOException {
            raFile.seek(start);

            return Channels.newInputStream(raFile.getChannel());
        }

        public ContentCreateOptions getCreateOptions() {
            return options;
        }

        public boolean isRewindable() {
            return true;
        }

        public void rewind() throws IOException {
            raFile.seek(start);
        }

        public long size() {
            try {
                return raFile.length() - start;
            } catch (IOException e) {
                return -1;
            }
        }

        public void close() {
            try {
                raFile.close();
            } catch (IOException e) {
                // smother it, we don't care
            }
        }
    }

    private static class InputStreamContent implements Content {
        private final String uri;
        private final ContentCreateOptions options;
        private InputStream is;

        public InputStreamContent(String uri, InputStream is, ContentCreateOptions options) {
            this.uri = uri;
            this.is = is;
            this.options = options;
        }

        public String getUri() {
            return uri;
        }

        public InputStream openDataStream() throws FileNotFoundException {
            if (is == null) {
                throw new IllegalStateException("Data stream has already been consumed");
            }

            InputStream isTmp = is;

            is = null;

            return isTmp;
        }

        public ContentCreateOptions getCreateOptions() {
            return options;
        }

        public boolean isRewindable() {
            return false;
        }

        public void rewind() throws IOException {
            if (is == null) {
                throw new IllegalStateException("This Content is not rewindable");
            }
        }

        public long size() {
            return -1;
        }

        public void close() {
            is = null;
        }
    }
}
