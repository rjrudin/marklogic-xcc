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

import com.marklogic.xcc.types.CtsBox;
import com.marklogic.xcc.types.ValueType;

public class CtsBoxImpl extends AbstractStringItem implements CtsBox {
    
    private String south;
    private String west;
    private String north;
    private String east;
    
    public CtsBoxImpl(String value) {
        super(ValueType.CTS_BOX, value);
    }

    public CtsBoxImpl(String south, String west, String north, String east) {
        super(ValueType.CTS_BOX, null);
        this.south = south;
        this.west = west;
        this.north = north;
        this.east = east;
    }
    
    private void parse() {
        try {
            int i = value.indexOf(',',1);
            south = value.substring(1,i);
            int j = value.indexOf(',', i+=2);
            west = value.substring(i,j);
            i = value.indexOf(',', j+=2);
            north = value.substring(j,i);
            j = value.indexOf(']', i+=2);
            east = value.substring(i,j);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse value as cts:box: " + value);
        }
    }
    
    @Override
    public String asString() {
        if (value == null) value = String.format("[%s, %s, %s, %s]", south, west, north, east);
        return value;
    }

    @Override
    public String getSouth() {
        if (south == null) parse();
        return south;
    }

    @Override
    public String getWest() {
        if (west == null) parse();
        return west;
    }

    @Override
    public String getNorth() {
        if (north == null) parse();
        return north;
    }

    @Override
    public String getEast() {
        if (east == null) parse();
        return east;
    }
}
