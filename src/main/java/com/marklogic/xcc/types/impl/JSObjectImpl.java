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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marklogic.xcc.types.JSObject;
import com.marklogic.xcc.types.JsonItem;
import com.marklogic.xcc.types.ValueType;

public class JSObjectImpl extends AbstractStringItem 
implements JSObject, JsonItem {

    public JSObjectImpl(String value) {
        super(ValueType.JS_OBJECT, value);
    }

    @Override
    public JsonNode asJsonNode() 
    throws JsonParseException, JsonMappingException, IOException {
        return asJsonNode(new ObjectMapper());
    }

    @Override
    public JsonNode asJsonNode(ObjectMapper mapper) 
    throws JsonParseException, JsonMappingException, IOException {
        return mapper.readValue(value, JsonNode.class);
    }
}
