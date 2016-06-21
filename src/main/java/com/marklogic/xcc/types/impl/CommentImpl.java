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

import org.w3c.dom.Comment;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.marklogic.xcc.types.ValueType;
import com.marklogic.xcc.types.XdmComment;

public class CommentImpl extends AbstractStreamableNodeItem implements XdmComment {
    
    public CommentImpl(String value) {
        super(ValueType.COMMENT, value);
    }

    public CommentImpl(InputStream stream) {
        super(ValueType.COMMENT, stream);
    }

    @Override
    public Node asW3cNode(DocumentBuilder docBuilder) throws IOException, SAXException {
        return asW3cComment(docBuilder);
    }

    @Override
    public Node asW3cNode() throws ParserConfigurationException, IOException, SAXException {
        return asW3cComment();
    }

    @Override
    public Comment asW3cComment(DocumentBuilder docBuilder) throws IOException,
            SAXException {
        String value = asString();
        return docBuilder.newDocument().createComment(value.substring(4,value.length()-3));
    }

    @Override
    public Comment asW3cComment() throws ParserConfigurationException, IOException,
            SAXException {
        return asW3cComment(DocumentBuilderFactory.newInstance().newDocumentBuilder());
    }
}
