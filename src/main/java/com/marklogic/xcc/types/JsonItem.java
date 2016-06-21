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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
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
