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
package com.marklogic.xcc.types.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.marklogic.io.IOHelper;
import com.marklogic.xcc.types.ValueType;
import com.marklogic.xcc.types.XdmBinary;

public class BinaryImpl extends AbstractItem implements XdmBinary, StreamableItem {
    private InputStream stream;
    private byte[] bytes = null;

    public BinaryImpl(InputStream bodyStream, boolean cache) {
        super(ValueType.BINARY);

        this.stream = bodyStream;

        if (cache) {
            asBinaryData(); // sucks the data off the stream and buffers it
        }
    }

    // -----------------------------------------------------
    // StreamableItem

    public boolean isFetchable() {
        return ((bytes != null) || (stream != null));
    }

    public void invalidate() {
        bytes = null;

        if (stream != null) {
            try {
                //noinspection ResultOfMethodCallIgnored
                stream.skip(Long.MAX_VALUE);
                stream.close();
            } catch (IOException e) {
                // do nothing, may have been closed already
            }

            stream = null;
        }
    }

    // -----------------------------------------------------

    public boolean isCached() {
        return (bytes != null);
    }

    public Reader asReader() {
        return new InputStreamReader(asInputStream());
    }

    public InputStream asInputStream() {
        if (bytes != null) {
            return new ByteArrayInputStream(bytes);
        }

        InputStream tmp = stream;

        stream = null;

        return (tmp);
    }

    public String asString() {
        try {
            return new String(asBinaryData(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return new String(asBinaryData());
        }
    }

    public byte[] asBinaryData() {
        if (bytes != null) {
            return bytes.clone();
        }

        if (stream == null) {
            throw new IllegalStateException("stream data has already been consumed");
        }

        try {
            bytes = IOHelper.byteArrayFromStream(stream);
        } catch (IOException e) {
            throw new RuntimeException("IOException buffering binary data", e);
        }

        stream = null;

        return bytes.clone();
    }

    public Node asW3cNode(DocumentBuilder docBuilder) throws IOException, SAXException {
        throw new UnsupportedOperationException("binary() cannot be converted to a W3C Node");
    }

    public Node asW3cNode() throws ParserConfigurationException, IOException, SAXException {
        return asW3cNode(null);
    }
}
