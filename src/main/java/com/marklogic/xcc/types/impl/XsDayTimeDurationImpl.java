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

import com.marklogic.xcc.types.ValueType;
import com.marklogic.xcc.types.XSDayTimeDuration;
import com.marklogic.xcc.types.XdmDuration;

public class XsDayTimeDurationImpl extends AbstractDurationItem implements XSDayTimeDuration {
    public XsDayTimeDurationImpl(String bodyString) {
        super(ValueType.XS_DAY_TIME_DURATION, bodyString);

        XdmDuration duration = asDuration();

        if ((duration.getYears() != 0) || (duration.getMonths() != 0)) {
            throw new IllegalArgumentException("Only Day and Time values are allowed");
        }

    }
}
