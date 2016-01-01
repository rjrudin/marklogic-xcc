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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

/**
 * An XDM value which is a document().
 */
public interface XdmDocument extends XdmNode {
    /**
     * <p>
     * Returns a W3C Document equivalent of this document. Buffers the document item from the server
     * and converts it to a W3C DOM Document object. The document is buffered as a String object.
     * Subsequent calls will create a new DOM tree from the same String. The buffered String may
     * also be used by {@link #asString()} and {@link #asInputStream()}.
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
     * @return This item as a W3C element (org.w3c.dom.Element) instance.
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
