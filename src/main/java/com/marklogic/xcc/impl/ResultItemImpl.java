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
package com.marklogic.xcc.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

import com.marklogic.xcc.ResultItem;
import com.marklogic.xcc.types.ItemType;
import com.marklogic.xcc.types.ValueType;
import com.marklogic.xcc.types.XdmItem;
import com.marklogic.xcc.types.impl.StreamableItem;

public class ResultItemImpl implements ResultItem {
    private final XdmItem value;
    private final int index;
    private final String uri;
    private final String path;

    // -------------------------------------------------

    public ResultItemImpl(XdmItem value, int index, String uri, String path) {
        this.value = value;
        this.index = index;
        this.uri = uri;
        this.path = ((uri != null) && (path == null)) ? "/" : path;
    }

    // -------------------------------------------------
    // ResultItem interface methods

	public String getDocumentURI() {
		return uri;
	}
	
	public String getNodePath() {
		return path;
	}

	public XdmItem getItem() {
        return value;
    }

    public int getIndex() {
        return index;
    }

    public boolean isFetchable() {
        if (value.isCached()) {
            return true;
        }

        if (value instanceof StreamableItem) {
            StreamableItem sitem = (StreamableItem)value;

            return (sitem.isFetchable());
        }

        return (false);
    }

    public void cache() {
        if (!value.isCached()) {
            asString();
        }
    }

    // -------------------------------------------------
    // XdmItem interface adapter

    public ItemType getItemType() {
        return value.getItemType();
    }

    public Reader asReader() {
        return value.asReader();
    }

    public InputStream asInputStream() {
        return value.asInputStream();
    }

    public String asString() {
        return value.asString();
    }

    public boolean isCached() {
        return value.isCached();
    }

    public void writeTo(Writer writer) throws IOException {
        value.writeTo(writer);
    }

    public void writeTo(OutputStream outputStream) throws IOException {
        value.writeTo(outputStream);
    }

    // -------------------------------------------------
    // XdmValue interface adapter

    public ValueType getValueType() {
        return value.getValueType();
    }
}
