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

/**
 * Type indicator class for values that are sequences.
 */
public final class SequenceType extends ValueType {
    SequenceType(String name) {
        super(name);
    }

    /**
     * Always returns true.
     * 
     * @return True to indicate that this is a sequence.
     */
    @Override
    public boolean isSequence() {
        return (true);
    }
}
