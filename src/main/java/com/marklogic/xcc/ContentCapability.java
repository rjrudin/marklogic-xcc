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
package com.marklogic.xcc;

/**
 * A typesafe enumeration class defining permission capability values.
 */
public final class ContentCapability {
    /** Reads are allowed */
    public static final ContentCapability READ = new ContentCapability("read", "R");

    /** Inserts are allowed */
    public static final ContentCapability INSERT = new ContentCapability("insert", "I");

    /** Updates are allowed */
    public static final ContentCapability UPDATE = new ContentCapability("update", "U");

    /** Execution is allowed */
    public static final ContentCapability EXECUTE = new ContentCapability("execute", "E");

    private String name;
    private String symbol;

    private ContentCapability(String name, String symbol) {
        this.name = name;
        this.symbol = symbol;
    }

    /**
     * Used internally.
     * 
     * @return A single-char encoding symbol.
     */
    public String getSymbol() {
        return symbol;
    }

    /**
     * A human-readable name.
     * 
     * @return A String name for this capability.
     */
    @Override
    public String toString() {
        return name;
    }
}
