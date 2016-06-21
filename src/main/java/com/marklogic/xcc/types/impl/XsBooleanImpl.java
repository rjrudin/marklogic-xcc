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

import com.marklogic.xcc.types.ValueType;
import com.marklogic.xcc.types.XSBoolean;

public class XsBooleanImpl extends AbstractStringItem implements XSBoolean {
    private Boolean value;

    public XsBooleanImpl(String value) {
        super(ValueType.XS_BOOLEAN, value);

        this.value = Boolean.valueOf(value);
    }

    public XsBooleanImpl(Boolean value) {
        super(ValueType.XS_BOOLEAN, value.toString());

        this.value = value;
    }

    public Boolean asBoolean() {
        return value;
    }

    public boolean asPrimitiveBoolean() {
        return value.booleanValue();
    }
}
