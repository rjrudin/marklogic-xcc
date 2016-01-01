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

import com.marklogic.xcc.types.CtsCircle;
import com.marklogic.xcc.types.CtsPoint;
import com.marklogic.xcc.types.ValueType;

public class CtsCircleImpl extends AbstractStringItem implements CtsCircle {
    
    private String radius;
    private CtsPoint center;
    
    public CtsCircleImpl(String value) {
        super(ValueType.CTS_CIRCLE, value);
    }

    public CtsCircleImpl(String radius, CtsPoint center) {
        super(ValueType.CTS_CIRCLE, null);
        this.radius = radius;
        this.center = center;
    }

    private void parse() {
        try {
            int i = value.indexOf(' ',1);
            radius = value.substring(1,i);
            center = new CtsPointImpl(value.substring(i+1));
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse value as cts:circle: " + value);
        }
    }
    
    @Override
    public String asString() {
        if (value == null) value = String.format("@%s %s,%s", radius, center.getLatitude(), center.getLongitude());
        return value;
    }

    @Override
    public String getRadius() {
        if (radius == null) parse();
        return radius;
    }

    @Override
    public CtsPoint getCenter() {
        if (center == null) parse();
        return center;
    }
}
