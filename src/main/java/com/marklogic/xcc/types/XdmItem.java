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
package com.marklogic.xcc.types;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

/**
 * Interface for item values (values that may be a member of a sequence).
 */
public interface XdmItem extends XdmValue {
    /**
     * The {@link ItemType} instance corresponding to the type of this item.
     * 
     * @return An instance of {@link ItemType} that represent the XML Schema type of this value.
     */
    ItemType getItemType();

    /**
     * Return a java.io.Reader for this item.
     * 
     * @return A Reader instance from which this value may be read. For most types, the characters
     *         read will be equivalent to those returned by {@link #asString()}.
     * @throws UnsupportedOperationException
     *             If the underlying value object cannot be represented as a character stream.
     */
    Reader asReader();

    /**
     * Return a java.io.InputStream for this item.
     * 
     * @return An InputStream instance from which this value may be read as a byte stream. Note that
     *         most simple types (xs:integer, xs:duration, etc) may not be represented as a byte
     *         stream. For string types (xs:string, xs:QName, etc) the byte stream will be UTF-8
     *         encoded).
     * @throws UnsupportedOperationException
     *             If the underlying value object cannot be represented as a byte stream.
     */
    InputStream asInputStream();

    /**
     * Write the value of this item to the provided {@link Writer} object. The {@link Reader}
     * returned by the method {@link #asReader()} will be used as the value source.
     * 
     * @param writer
     *            A client-provided, open {@link Writer} object to which the value will be written.
     *            The {@link Writer} will not be closed by this method.
     */
    void writeTo(Writer writer) throws IOException;

    /**
     * Write the value of this item to the provided {@link OutputStream} object. The
     * {@link InputStream} returned by the method {@link #asInputStream} will be as the value
     * source.
     * 
     * @param outputStream
     *            A Client-provided, open {@link OutputStream} object to which the value will be
     *            written. The {@link OutputStream} will not be closed by this method.
     */
    void writeTo(OutputStream outputStream) throws IOException;

    /**
     * <p>
     * Indicates whether this item's value is buffered in memory. If so it is safe to call any of
     * the accessor methods ({@link #asString()}, {@link #asInputStream()} or {@link #asReader()})
     * and to call them repeatedly.
     * </p>
     * <p>
     * Normally this method will only return false for {@link com.marklogic.xcc.ResultItem}
     * instances which are members of a streaming {@link com.marklogic.xcc.ResultSequence}s.
     * However, even such instances may be indicate thstt they are cached if, for example,
     * {@link #asString()} is the first method invoked. Doing so will buffer the value which can
     * then be reused for subsequent invocations of {@link #asReader()} or {@link #asInputStream()}.
     * </p>
     * 
     * @return true if the value is buffered and may be retrieved repeatedly, false if not.
     */
    boolean isCached();
}
