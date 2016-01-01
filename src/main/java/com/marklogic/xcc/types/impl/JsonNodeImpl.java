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
