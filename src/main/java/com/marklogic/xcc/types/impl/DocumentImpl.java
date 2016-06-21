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
package com.marklogic.xcc.types.impl;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.marklogic.xcc.types.ValueType;
import com.marklogic.xcc.types.XdmDocument;

public class DocumentImpl extends AbstractStreamableNodeItem implements XdmDocument {
    public DocumentImpl(String value) {
        super(ValueType.DOCUMENT, value);
    }

    public DocumentImpl(InputStream stream) {
        super(ValueType.DOCUMENT, stream);
    }

    @Override
    public Document asW3cDocument(DocumentBuilder docBuilder) throws IOException, SAXException {
        return docBuilder.parse(asInputStream());
    }

    @Override
    public Document asW3cDocument() throws ParserConfigurationException, IOException, SAXException {
        return asW3cDocument(DocumentBuilderFactory.newInstance().newDocumentBuilder());
    }

    @Override
    public Node asW3cNode(DocumentBuilder docBuilder) throws IOException, SAXException {
        return asW3cDocument(docBuilder);
    }

    @Override
    public Node asW3cNode() throws ParserConfigurationException, IOException, SAXException {
        return asW3cDocument();
    }
}
