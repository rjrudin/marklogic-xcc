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
import com.marklogic.xcc.types.XSDecimal;

public class XsDecimalImpl extends AbstractStringItem implements XSDecimal {
    private final BigDecimal value;

    public XsDecimalImpl(String bodyString) {
        super(ValueType.XS_DECIMAL, bodyString);

        this.value = new BigDecimal(bodyString);
    }

    public XsDecimalImpl(Object value) {
        super(ValueType.XS_DECIMAL, value.toString());

        if ((value instanceof Integer) || (value instanceof Long) || (value instanceof Double)
                || (value instanceof Float) || (value instanceof BigDecimal) || (value instanceof BigInteger)
                || (value instanceof String)) {
            this.value = new BigDecimal(value.toString());
        } else {
            throw new IllegalArgumentException("Cannot construct XSDecimal from " + value.getClass().getName());
        }
    }

    public BigDecimal asBigDecimal() {
        return value;
    }
}
