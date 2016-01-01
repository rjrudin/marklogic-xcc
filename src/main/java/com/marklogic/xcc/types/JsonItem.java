package com.marklogic.xcc.types;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Interface for JSON item values.
 */
public interface JsonItem extends XdmItem {
    /**
     * @return The value of this item as a JsonNode object.
     */
    JsonNode asJsonNode() 
    throws JsonParseException, JsonMappingException, IOException;
    
    /**
     * @return The value of this item as a JsonNode object.
     */
    JsonNode asJsonNode(ObjectMapper mapper)
    throws JsonParseException, JsonMappingException, IOException;
}
