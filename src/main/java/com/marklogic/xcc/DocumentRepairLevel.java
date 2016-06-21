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
 * <p>
 * A typesafe enumeration class defining load-time document repair levels.
 * </p>
 */
public final class DocumentRepairLevel {
    /** No automatic repair, insert as-is or error out if not well formed. */
    public static final DocumentRepairLevel NONE = new DocumentRepairLevel("none");

    /** Repair document to make it well formed, if possible. */
    public static final DocumentRepairLevel FULL = new DocumentRepairLevel("full");

    /**
     * Use server's default repair mode, which depends on the App Server default XQuery version
     * setting: 'none' for 1.0-ml or 1.0, 'full' for 0.9-ml or for MarkLogic Server releases prior
     * to 4.0.
     */
    public static final DocumentRepairLevel DEFAULT = new DocumentRepairLevel("default");

    private String name;

    private DocumentRepairLevel(String name) {
        this.name = name;
    }

    /**
     * The name of this repair level: "none" or "full".
     * 
     * @return The name of this document repair level, for diagnostic purposes.
     */
    @Override
    public String toString() {
        return (name);
    }
}
