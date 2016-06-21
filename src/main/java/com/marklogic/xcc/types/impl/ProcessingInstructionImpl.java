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

import org.w3c.dom.Node;
import org.w3c.dom.ProcessingInstruction;
import org.xml.sax.SAXException;

import com.marklogic.xcc.types.ValueType;
import com.marklogic.xcc.types.XdmProcessingInstruction;

public class ProcessingInstructionImpl extends AbstractStreamableNodeItem implements XdmProcessingInstruction {
    
    public ProcessingInstructionImpl(String value) {
        super(ValueType.PROCESSING_INSTRUCTION, value);
    }

    public ProcessingInstructionImpl(InputStream stream) {
        super(ValueType.PROCESSING_INSTRUCTION, stream);
    }

    @Override
    public Node asW3cNode(DocumentBuilder docBuilder) throws IOException, SAXException {
        return asW3cProcessingInstruction(docBuilder);
    }

    @Override
    public Node asW3cNode() throws ParserConfigurationException, IOException, SAXException {
        return asW3cProcessingInstruction();
    }

    @Override
    public ProcessingInstruction asW3cProcessingInstruction(DocumentBuilder docBuilder) throws IOException,
            SAXException {
        String value = asString();
        String content = value.substring(2,value.length()-2);
        String parts[] = content.split("[  \t\r\n]+",2);
        return docBuilder.newDocument().createProcessingInstruction(parts[0], parts.length < 2 ? null : parts[1]);
    }

    @Override
    public ProcessingInstruction asW3cProcessingInstruction() throws ParserConfigurationException, IOException,
            SAXException {
        return asW3cProcessingInstruction(DocumentBuilderFactory.newInstance().newDocumentBuilder());
    }
}
