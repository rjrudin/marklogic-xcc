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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

/**
 * Each instance of this class corresponds to an XQuery schema type.
 */
public abstract class ItemType extends ValueType {
    private static final Map<String, ItemType> typeMap = new HashMap<String, ItemType>();

    // --------------------------------------------------

    ItemType(String name) {
        super(name);
    }

    // --------------------------------------------------

    /**
     * Always returns false.
     * 
     * @return Always false for XdmItem types.
     */
    @Override
    public final boolean isSequence() {
        return (false);
    }

    // --------------------------------------------------

    /**
     * Implemented by subclasses.
     * 
     * @return True for node item types, false otherwise.
     */
    public abstract boolean isNode();

    /**
     * Implemented by subclasses.
     * 
     * @return True for atomic item types, false otherwise.
     */
    public abstract boolean isAtomic();

    // --------------------------------------------------

    /**
     * @param schemaTypeName
     *            An XML Schema type name, such as "xs:integer".
     * @return A typesafe enumeration member that represents the type, or null if the type name is
     *         not recognized. Note that type names are case-sensitive.
     */
    public static ItemType forType(String schemaTypeName) {
        return typeMap.get(schemaTypeName);
    }

    static {
        Field[] fields = ValueType.class.getFields();

        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            Object value = null;

            if (!Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            try {
                value = field.get(NODE); // specific instance is irrelevant

                if (value instanceof ItemType) {
                    typeMap.put(value.toString(), (ItemType)value);
                }
            } catch (IllegalAccessException e) {
                // nothing
            }
        }
    }
}
