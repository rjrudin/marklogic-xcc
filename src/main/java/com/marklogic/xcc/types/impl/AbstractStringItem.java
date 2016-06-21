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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;

import com.marklogic.xcc.types.ItemType;

abstract class AbstractStringItem extends AbstractItem {
    protected String value;

    protected AbstractStringItem(ItemType type, String value) {
        super(type);

        this.value = value;
    }

    public boolean isCached() {
        return (true);
    }

    public String asString() {
        return value;
    }

    public Reader asReader() {
        return new StringReader(asString());
    }

    public InputStream asInputStream() {
        try {
            return new ByteArrayInputStream(asString().getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            // This is unlikely to happen, UTF-8 is a required encoding
            return new ByteArrayInputStream(asString().getBytes());
        }
    }

    // -------------------------------------------------

    protected String scrubbedFloatValue(String rawValue) {
        if (rawValue.equalsIgnoreCase("-INF"))
            return "-Infinity";
        if (rawValue.equalsIgnoreCase("+INF"))
            return "+Infinity";
        if (rawValue.equalsIgnoreCase("INF"))
            return "Infinity";

        return rawValue;
    }

    // -------------------------------------------------

    @Override
    public String toString() {
        return asString();
    }
}
