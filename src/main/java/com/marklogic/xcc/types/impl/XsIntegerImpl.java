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

import java.math.BigInteger;

import com.marklogic.xcc.types.ValueType;
import com.marklogic.xcc.types.XSInteger;

public class XsIntegerImpl extends AbstractStringItem implements XSInteger {
    private BigInteger value;

    public XsIntegerImpl(String value) {
        super(ValueType.XS_INTEGER, value);

        this.value = new BigInteger(value);
    }

    public XsIntegerImpl(Object value) {
        super(ValueType.XS_INTEGER, value.toString());

        if ((value instanceof Integer) || (value instanceof Long) || (value instanceof BigInteger)
                || (value instanceof String)) {
            this.value = new BigInteger(value.toString());
        } else {
            throw new IllegalArgumentException("Cannot construct XSInteger from " + value.getClass().getName());
        }
    }

    public BigInteger asBigInteger() {
        return (value);
    }

    public Long asLong() {
        return new Long(value.longValue());
    }

    public long asPrimitiveLong() {
        return value.longValue();
    }

    public Integer asInteger() {
        return Integer.valueOf(asString());
    }

    public int asPrimitiveInt() {
        return (int)asPrimitiveLong();
    }
}
