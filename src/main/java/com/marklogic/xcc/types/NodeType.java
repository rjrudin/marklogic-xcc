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
package com.marklogic.xcc.types;

/**
 * Base type for enumerators that represent node types.
 */
public final class NodeType extends ItemType {
    NodeType(String name) {
        super(name);
    }

    // --------------------------------------------------

    /**
     * Always true.
     * 
     * @return Always true for node types.
     */
    @Override
    public boolean isNode() {
        return (true);
    }

    /**
     * Always false.
     * 
     * @return Always false for node types.
     */
    @Override
    public boolean isAtomic() {
        return (false);
    }

    // --------------------------------------------------

}
