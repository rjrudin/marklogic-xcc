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

import com.marklogic.xcc.types.ValueType;
import com.marklogic.xcc.types.XSYearMonthDuration;
import com.marklogic.xcc.types.XdmDuration;

public class XsYearMonthDurationImpl extends AbstractDurationItem implements XSYearMonthDuration {
    public XsYearMonthDurationImpl(String bodyString) {
        super(ValueType.XS_YEAR_MONTH_DURATION, bodyString);

        XdmDuration duration = asDuration();

        if ((duration.getDays() != 0) || (duration.getHours() != 0) || (duration.getMinutes() != 0)
                || (!duration.getSeconds().equals(new BigDecimal("0.0")))) {
            throw new IllegalArgumentException("Only Year and Month values are allowed");
        }
    }
}
