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
 * Typesafe enumeration of allowable formats for document insertion.
 */
public final class DocumentFormat {
    /** Document format = XML node() */
    public static final DocumentFormat XML = new DocumentFormat("xml");
    
    /** Document format = JSON node() */
    public static final DocumentFormat JSON = new DocumentFormat("json");

    /** Document format = text() */
    public static final DocumentFormat TEXT = new DocumentFormat("text()");

    /** Document format = binary() */
    public static final DocumentFormat BINARY = new DocumentFormat("binary()");

    /** Document format = none (use server default) */
    public static final DocumentFormat NONE = new DocumentFormat("(none)");

    private String name;

    private DocumentFormat(String name) {
        this.name = name;
    }

    /**
     * The name of this format: "xml", "text" or "binary".
     * 
     * @return The name of this format as a String, for diagnostic purposes.
     */
    @Override
    public String toString() {
        return (name);
    }
}
