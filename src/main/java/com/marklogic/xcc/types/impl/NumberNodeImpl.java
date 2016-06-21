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

import java.io.InputStream;
import java.math.BigDecimal;

import com.marklogic.xcc.types.NumberNode;
import com.marklogic.xcc.types.ValueType;

public class NumberNodeImpl extends JsonNodeImpl implements
        NumberNode {
    private final double value;
    private final BigDecimal bigDecimalValue;
    private final NumberFormatException formatException;

    public NumberNodeImpl(InputStream stream) {
        super(ValueType.NUMBER_NODE, stream);
        
        value = Double.valueOf(scrubbedFloatValue(asString()));
        BigDecimal tmpBigDecimal = null;
        NumberFormatException tmpEx = null;

        try {
            tmpBigDecimal = new BigDecimal(asString());
        } catch (NumberFormatException e) {
            // Double value parsed, must be NaN or +-INF, leave as null
            tmpEx = e;
        }

        this.bigDecimalValue = tmpBigDecimal;
        this.formatException = tmpEx;
    }
    
    public NumberNodeImpl(String stringVal) {
        super(ValueType.NUMBER_NODE, stringVal);
        
        value = Double.valueOf(scrubbedFloatValue(stringVal));
        BigDecimal tmpBigDecimal = null;
        NumberFormatException tmpEx = null;

        try {
            tmpBigDecimal = new BigDecimal(stringVal);
        } catch (NumberFormatException e) {
            // Double value parsed, must be NaN or +-INF, leave as null
            tmpEx = e;
        }

        this.bigDecimalValue = tmpBigDecimal;
        this.formatException = tmpEx;
    }

    public double asDouble() {
        return value;
    }

    public BigDecimal asBigDecimal() {
        if (bigDecimalValue == null) {
            throw formatException;
        }
        return bigDecimalValue;
    }
    
    String scrubbedFloatValue(String rawValue) {
        if (rawValue.equalsIgnoreCase("-INF"))
            return "-Infinity";
        if (rawValue.equalsIgnoreCase("+INF"))
            return "+Infinity";
        if (rawValue.equalsIgnoreCase("INF"))
            return "Infinity";

        return rawValue;
    }

}
