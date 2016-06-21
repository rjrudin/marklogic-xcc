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
package com.marklogic.xcc.types;

import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

/**
 * An XQuery value which is an element().
 */
public interface XdmElement extends XdmNode {
    /**
     * <p>
     * Returns a W3C Element object equivalent to this XdmElement. Buffers the element() item from
     * the server and converts it to a W3C Element DOM object. The element() is buffered as a String
     * object. Subsequent calls will create a new DOM tree from the same String. The buffered String
     * may also be used by {@link #asString()} and {@link #asInputStream()}.
     * </p>
     * 
     * @param docBuilder
     *            The javax.xml.parsers.DocumentBuilder object to use to construct the Document. If
     *            null, the default implementation will be used. See the JDK documentation for the
     *            javax.xml.parsers.DocumentBuilderFactory class for details on configuring the
     *            system default builder.
     * @return This item as a W3C element (org.w3c.dom.Element) instance.
     * @throws IllegalStateException
     *             If called after the InputStream has already been consumed.
     * @see #asInputStream()
     * @see #asString()
     * @see #isCached()
     * @see #asW3cNode()
     */
    org.w3c.dom.Element asW3cElement(DocumentBuilder docBuilder) throws IOException, SAXException;

    /**
     * This is equivalent to <code>asW3cElement (null)</code>.
     * 
     * @return This item as a W3C element (org.w3c.dom.Element) instance.
     */
    org.w3c.dom.Element asW3cElement() throws ParserConfigurationException, IOException, SAXException;

    /**
     * <p>
     * Returns a W3C Document object with this element() as the root node. Buffers the element()
     * item from the server and converts it to a W3C Element DOM object. The element() is buffered
     * as a String object. Subsequent calls will create a new DOM tree from the same String. The
     * buffered String may also be used by {@link #asString()} and {@link #asInputStream()}.
     * </p>
     * <p>
     * If you are using JDOM and want to create a JDOM Document for this node, do the following:
     * <code>doc = new org.jdom.input.SAXBuilder().build (new StringReader (node.asString()))</code>
     * </p>
     * 
     * @param docBuilder
     *            The javax.xml.parsers.DocumentBuilder object to use to construct the Document. If
     *            null, the default implementation will be used. See the JDK documentation for the
     *            javax.xml.parsers.DocumentBuilderFactory class for details on configuring the
     *            system default builder.
     * @return This item as a W3C document (org.w3c.dom.Document) instance.
     * @throws IllegalStateException
     *             If called after the InputStream has already been consumed.
     * @see #asInputStream()
     * @see #asString()
     * @see #isCached()
     * @see #asW3cNode()
     */
    org.w3c.dom.Document asW3cDocument(DocumentBuilder docBuilder) throws IOException, SAXException;

    /**
     * This is equivalent to <code>asW3cDocument (null)</code>.
     * 
     * @return This item as a W3C document (org.w3c.dom.Document) instance.
     */
    org.w3c.dom.Document asW3cDocument() throws ParserConfigurationException, IOException, SAXException;
}
