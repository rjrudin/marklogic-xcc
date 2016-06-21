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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

import com.marklogic.xcc.types.ItemType;
import com.marklogic.xcc.types.ValueType;
import com.marklogic.xcc.types.XdmItem;

abstract class AbstractItem implements XdmItem {
    private final ItemType type;

    public AbstractItem(ItemType type) {
        this.type = type;
    }

    // -----------------------------------------------

    // defined in XdmItem interface and thus implicitly abstract
//	abstract public Reader asReader();
//	abstract public InputStream asInputStream();

    // -----------------------------------------------

    public void writeTo(Writer writer) throws IOException {
        Reader reader = asReader();
        char[] buffer = new char[64 * 1024];
        int rc;

        while ((rc = reader.read(buffer)) != -1) {
            writer.write(buffer, 0, rc);
        }

        writer.flush();
        reader.close();
    }

    public void writeTo(OutputStream outputStream) throws IOException {
        InputStream inputStream = asInputStream();
        byte[] buffer = new byte[64 * 1024];
        int rc;

        while ((rc = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, rc);
        }

        outputStream.flush();
        inputStream.close();
    }

    // -----------------------------------------------

    public ValueType getValueType() {
        return type;
    }

    public ItemType getItemType() {
        return type;
    }

    // -----------------------------------------------
}
