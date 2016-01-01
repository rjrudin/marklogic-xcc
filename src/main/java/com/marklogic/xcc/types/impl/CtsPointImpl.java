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

import com.marklogic.xcc.types.CtsPoint;
import com.marklogic.xcc.types.ValueType;

public class CtsPointImpl extends AbstractStringItem implements CtsPoint {
    
    private String latitude;
    private String longitude;
    
    public CtsPointImpl(String value) {
        super(ValueType.CTS_POINT, value);
    }

    public CtsPointImpl(String latitude, String longitude) {
        super(ValueType.CTS_POINT, null);
        this.latitude = latitude;
        this.longitude = longitude;
    }

    private void parse() {
        int i = value.indexOf(',');
        latitude = value.substring(0,i);
        longitude = value.substring(i+1);
    }
    
    @Override
    public String asString() {
        if (value == null) value = String.format("%s,%s", latitude, longitude);
        return value;
    }

    @Override
    public String getLongitude() {
        if (longitude == null) parse();
        return longitude;
    }

    @Override
    public String getLatitude() {
        if (latitude == null) parse();
        return latitude;
    }
}
