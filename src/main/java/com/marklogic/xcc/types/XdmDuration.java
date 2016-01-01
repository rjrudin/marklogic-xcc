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
package com.marklogic.xcc.types;

import java.math.BigDecimal;

/**
 * A Java representation of xs:duration.
 */
public interface XdmDuration {
    boolean isPositive();

    boolean isNegative();

    int getYears();

    int getMonths();

    int getDays();

    int getHours();

    int getMinutes();

    long getWholeSeconds();

    BigDecimal getSeconds();
}
