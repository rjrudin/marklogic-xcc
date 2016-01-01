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

import java.math.BigDecimal;
import java.math.BigInteger;

import com.marklogic.xcc.types.ValueType;
import com.marklogic.xcc.types.XSDouble;

public class XsDoubleImpl extends AbstractStringItem implements XSDouble {
    private final Double value;
    private final BigDecimal bigDecimalValue;
    private final NumberFormatException formatException;

    public XsDoubleImpl(String bodyString) {
        super(ValueType.XS_DOUBLE, bodyString);

        this.value = Double.valueOf(scrubbedFloatValue(bodyString));

        BigDecimal tmpBigDecimal = null;
        NumberFormatException tmpEx = null;

        try {
            tmpBigDecimal = new BigDecimal(bodyString);
        } catch (NumberFormatException e) {
            // Double value parsed, must be NaN or +-INF, leave as null
            tmpEx = e;
        }

        this.bigDecimalValue = tmpBigDecimal;
        this.formatException = tmpEx;
    }

    public XsDoubleImpl(Object value) {
        super(ValueType.XS_DOUBLE, value.toString());

        if ((value instanceof Integer) || (value instanceof Long) || (value instanceof Double)
                || (value instanceof Float) || (value instanceof BigDecimal) || (value instanceof BigInteger)
                || (value instanceof String)) {
            this.value = new Double(value.toString());

            BigDecimal tmpBigDecimal = null;
            NumberFormatException tmpEx = null;

            try {
                tmpBigDecimal = new BigDecimal(value.toString());
            } catch (NumberFormatException e) {
                tmpEx = e;
            }

            this.bigDecimalValue = tmpBigDecimal;
            this.formatException = tmpEx;
        } else {
            throw new IllegalArgumentException("Cannot construct XSDouble from " + value.getClass().getName());
        }
    }

    public Double asDouble() {
        return (value);
    }

    public double asPrimitiveDouble() {
        return value.doubleValue();
    }

    public BigDecimal asBigDecimal() {
        if (bigDecimalValue == null) {
            throw formatException;
        }

        return bigDecimalValue;
    }
}
