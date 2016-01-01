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

import java.util.ArrayList;
import java.util.List;

import com.marklogic.xcc.types.CtsPoint;
import com.marklogic.xcc.types.CtsPolygon;
import com.marklogic.xcc.types.ValueType;

public class CtsPolygonImpl extends AbstractStringItem implements CtsPolygon {
    
    private List<CtsPoint> vertices;
    
    public CtsPolygonImpl(String value) {
        super(ValueType.CTS_POLYGON, value);
    }
        
    public CtsPolygonImpl(List<CtsPoint> vertices) {
        super(ValueType.CTS_POLYGON, vertices.size() > 0 ? null : "");
        this.vertices = vertices;
    }

    private void parse() {
        int count = 0;
        int i = 0;
        while ((i=value.indexOf(' ',i)+1)>0) count++;
        vertices = new ArrayList<CtsPoint>(count);
        
        i = 0;
        while (count-- > 0){
            int j = value.indexOf(' ',i);
            vertices.add(new CtsPointImpl(value.substring(i,j)));
            i = j+1;
        }
        
        if (i > 0) {
            vertices.add(new CtsPointImpl(value.substring(i)));
        }
    }
    
    @Override
    public String asString() {
        if (value == null) {
            StringBuilder sb = new StringBuilder();
            for (CtsPoint vertex : vertices) { 
                sb.append(vertex.getLatitude());
                sb.append(",");
                sb.append(vertex.getLongitude());
                sb.append(" ");
            }
            value = sb.substring(0, sb.length()-1);
        }
        return value;
    }

    @Override
    public List<CtsPoint> getVertices() {
        if (vertices == null) parse();
        return vertices;
    }
}
