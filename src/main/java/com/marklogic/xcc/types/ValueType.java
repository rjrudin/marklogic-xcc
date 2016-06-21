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
 * Base class for the typesafe enumeration objects that represent XML Schema types.
 */
public abstract class ValueType {
    private String name;

    ValueType(String name) {
        this.name = name;
    }

    /**
     * Indicates whether this value type represents a sequence or single value.
     * 
     * @return true if the type represented is a sequence, false otherwise.
     */
    public abstract boolean isSequence();

    /**
     * The name of this XQuery type, as a string.
     * 
     * @return A string represntation of this type.
     */
    @Override
    public String toString() {
        return (name);
    }

    // -----------------------------------------------------

    public static final ValueType SEQUENCE = new SequenceType("(sequence)");

    public static final NodeType NODE = new NodeType("node()");
    public static final NodeType ELEMENT = new NodeType("element()");
    public static final NodeType DOCUMENT = new NodeType("document-node()");
    public static final NodeType TEXT = new NodeType("text()");
    public static final NodeType BINARY = new NodeType("binary()");
    public static final NodeType ATTRIBUTE = new NodeType("attribute()");
    public static final NodeType PROCESSING_INSTRUCTION = new NodeType("processing-instruction()");
    public static final NodeType COMMENT = new NodeType("comment()");

    public static final NodeType OBJECT_NODE = new NodeType("object-node()");
    public static final NodeType NULL_NODE = new NodeType("null-node()");
    public static final NodeType BOOLEAN_NODE = new NodeType("boolean-node()");
    public static final NodeType ARRAY_NODE = new NodeType("array-node()");
    public static final NodeType NUMBER_NODE = new NodeType("number-node()");
    
    public static final AtomicType XS_UNTYPED_ATOMIC = new AtomicType("xs:untypedAtomic");
    public static final AtomicType XS_STRING = new AtomicType("xs:string");
    public static final AtomicType XS_BOOLEAN = new AtomicType("xs:boolean");
    public static final AtomicType XS_INTEGER = new AtomicType("xs:integer");
    public static final AtomicType XS_DECIMAL = new AtomicType("xs:decimal");
    public static final AtomicType XS_FLOAT = new AtomicType("xs:float");
    public static final AtomicType XS_DOUBLE = new AtomicType("xs:double");
    public static final AtomicType XS_DURATION = new AtomicType("xs:duration");
    public static final AtomicType XS_DAY_TIME_DURATION = new AtomicType("xs:dayTimeDuration");
    public static final AtomicType XS_YEAR_MONTH_DURATION = new AtomicType("xs:yearMonthDuration");
    public static final AtomicType XS_DATE_TIME = new AtomicType("xs:dateTime");
    public static final AtomicType XS_TIME = new AtomicType("xs:time");
    public static final AtomicType XS_DATE = new AtomicType("xs:date");
    public static final AtomicType XS_ANY_URI = new AtomicType("xs:anyURI");
    public static final AtomicType XS_QNAME = new AtomicType("xs:QName");
    public static final AtomicType XS_GDAY = new AtomicType("xs:gDay");
    public static final AtomicType XS_GMONTH = new AtomicType("xs:gMonth");
    public static final AtomicType XS_GMONTH_DAY = new AtomicType("xs:gMonthDay");
    public static final AtomicType XS_GYEAR = new AtomicType("xs:gYear");
    public static final AtomicType XS_GYEAR_MONTH = new AtomicType("xs:gYearMonth");
    public static final AtomicType XS_HEX_BINARY = new AtomicType("xs:hexBinary");
    public static final AtomicType XS_BASE64_BINARY = new AtomicType("xs:base64Binary");
    
    public static final AtomicType CTS_BOX = new AtomicType("cts:box");
    public static final AtomicType CTS_CIRCLE = new AtomicType("cts:circle");
    public static final AtomicType CTS_POINT = new AtomicType("cts:point");
    public static final AtomicType CTS_POLYGON = new AtomicType("cts:polygon");
    
    public static final AtomicType JS_ARRAY = new AtomicType("json:array");
    public static final AtomicType JS_OBJECT = new AtomicType("json:object");
    
    public static final ValueType valueOf(String name) {
        if (name.startsWith("xs:")) {
            if (name.equals("xs:untypedAtomic")) return XS_UNTYPED_ATOMIC;
            else if (name.equals("xs:string")) return XS_STRING;
            else if (name.equals("xs:boolean")) return XS_BOOLEAN;
            else if (name.equals("xs:integer")) return XS_INTEGER;
            else if (name.equals("xs:decimal")) return XS_DECIMAL;
            else if (name.equals("xs:float")) return XS_FLOAT;
            else if (name.equals("xs:double")) return XS_DOUBLE;
            else if (name.equals("xs:duration")) return XS_DURATION;
            else if (name.equals("xs:dayTimeDuration")) return XS_DAY_TIME_DURATION;
            else if (name.equals("xs:yearMonthDuration")) return XS_YEAR_MONTH_DURATION;
            else if (name.equals("xs:dateTime")) return XS_DATE_TIME;
            else if (name.equals("xs:time")) return XS_TIME;
            else if (name.equals("xs:date")) return XS_DATE;
            else if (name.equals("xs:anyURI")) return XS_ANY_URI;
            else if (name.equals("xs:QName")) return XS_QNAME;
            else if (name.equals("xs:gDay")) return XS_GDAY;
            else if (name.equals("xs:gMonth")) return XS_GMONTH;
            else if (name.equals("xs:gMonthDay")) return XS_GMONTH_DAY;
            else if (name.equals("xs:gYear")) return XS_GYEAR;
            else if (name.equals("xs:gYearMonth")) return XS_GYEAR_MONTH;
            else if (name.equals("xs:hexBinary")) return XS_HEX_BINARY;
            else if (name.equals("xs:base64Binary")) return XS_BASE64_BINARY;
            else throw new IllegalArgumentException("Illegal value type: " + name);
        } else if (name.startsWith("cts:")) {
            if (name.equals("cts:box")) return CTS_BOX;
            else if (name.equals("cts:circle")) return CTS_CIRCLE;
            else if (name.equals("cts:point")) return CTS_POINT;
            else if (name.equals("cts:polygon")) return CTS_POLYGON;
            else throw new IllegalArgumentException("Illegal value type: " + name);
        } else if (name.startsWith("json")) {
            if (name.equals("json:object")) return JS_OBJECT;
            else if (name.equals("json:array")) return JS_ARRAY;
            else throw new IllegalArgumentException("Illegal value type: " + name);
        } else if (name.endsWith("()")) {
            if (name.equals("node()")) return NODE;
            else if (name.equals("element()")) return ELEMENT;
            else if (name.equals("document-node()")) return DOCUMENT;
            else if (name.equals("text()")) return TEXT;
            else if (name.equals("binary()")) return BINARY;
            else if (name.equals("attribute()")) return ATTRIBUTE;
            else if (name.equals("processing-instruction()")) return PROCESSING_INSTRUCTION;
            else if (name.equals("comment()")) return new NodeType(name);
            else if (name.equals("object-node()")) return OBJECT_NODE;
            else if (name.equals("array-node()")) return ARRAY_NODE;
            else if (name.equals("null-node()")) return NULL_NODE;
            else if (name.equals("number-node()")) return NUMBER_NODE;
            else if (name.equals("boolean-node()")) return BOOLEAN_NODE;
            else throw new IllegalArgumentException("Illegal value type: " + name);
        } else if (name.equals("(sequence)")) {
            return SEQUENCE;
        } else {
            throw new IllegalArgumentException("Illegal value type: " + name);
        }
    }
}
