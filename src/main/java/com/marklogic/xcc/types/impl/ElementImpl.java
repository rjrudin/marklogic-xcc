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

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.marklogic.xcc.types.ValueType;
import com.marklogic.xcc.types.XdmElement;

public class ElementImpl extends AbstractStreamableNodeItem implements XdmElement {
    public ElementImpl(String value) {
        super(ValueType.ELEMENT, value);
    }

    public ElementImpl(InputStream stream) {
        super(ValueType.ELEMENT, stream);
    }

    public Node asW3cNode(DocumentBuilder docBuilder) throws IOException, SAXException {
        return asW3cElement(docBuilder);
    }

    public Node asW3cNode() throws ParserConfigurationException, IOException, SAXException {
        return asW3cElement();
    }

    public Element asW3cElement(DocumentBuilder docBuilder) throws IOException, SAXException {
        return asW3cDocument(docBuilder).getDocumentElement();
    }

    public Element asW3cElement() throws ParserConfigurationException, IOException, SAXException {
        return asW3cElement(DocumentBuilderFactory.newInstance().newDocumentBuilder());
    }

    public Document asW3cDocument(DocumentBuilder docBuilder) throws IOException, SAXException {
        return docBuilder.parse(asInputStream());
    }

    public Document asW3cDocument() throws ParserConfigurationException, IOException, SAXException {
        return asW3cDocument(DocumentBuilderFactory.newInstance().newDocumentBuilder());
    }
}
