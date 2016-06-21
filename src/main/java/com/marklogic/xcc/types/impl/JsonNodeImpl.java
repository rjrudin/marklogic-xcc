package com.marklogic.xcc.types.impl;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marklogic.xcc.types.ItemType;
import com.marklogic.xcc.types.JsonItem;

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
public class JsonNodeImpl extends AbstractStreamableNodeItem 
implements JsonItem {

	public JsonNodeImpl(ItemType type, InputStream stream) {
		super(type, stream);
	}
	
	public JsonNodeImpl(ItemType type, String stringVal) {
		super(type, stringVal);
	}

	@Override
	public Node asW3cNode(DocumentBuilder docBuilder) throws IOException,
			SAXException {
		throw new UnsupportedOperationException("JSON node");
	}

	@Override
	public Node asW3cNode() throws ParserConfigurationException, IOException,
			SAXException {
		throw new UnsupportedOperationException("JSON node");
	}

	@Override
	public JsonNode asJsonNode() throws JsonParseException,
			JsonMappingException, IOException {
	    return asJsonNode(new ObjectMapper());
	}

	@Override
	public JsonNode asJsonNode(ObjectMapper mapper) throws JsonParseException,
			JsonMappingException, IOException {
		return mapper.readValue(asInputStream(), JsonNode.class);
	}
}
