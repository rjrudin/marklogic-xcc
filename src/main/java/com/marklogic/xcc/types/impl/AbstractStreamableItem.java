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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;

import com.marklogic.io.IOHelper;
import com.marklogic.xcc.types.ItemType;

public class AbstractStreamableItem extends AbstractItem implements StreamableItem {
    protected String stringVal = null;
    protected InputStream stream = null;

    public AbstractStreamableItem(ItemType type, String stringVal) {
        super(type);

        this.stringVal = stringVal;
    }

    public AbstractStreamableItem(ItemType type, InputStream stream) {
        super(type);

        this.stream = stream;
    }

    // -------------------------------------------------------------
    // StreamableItem

    public boolean isFetchable() {
        return ((stringVal != null) || (stream != null));
    }

    // TODO: test this
    public void invalidate() {
        stringVal = null;

        if (stream != null) {
            try {
                //noinspection ResultOfMethodCallIgnored
                stream.skip(Long.MAX_VALUE);
                stream.close();
            } catch (IOException e) {
                // do nothing, may have been closed already
            }

            stream = null;
        }
    }

    // -------------------------------------------------------------

    public boolean isCached() {
        return stringVal != null;
    }

    public Reader asReader() {
        if (stream != null) {
            InputStream tmp = stream;

            stream = null;

            try {
                return (new InputStreamReader(tmp, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                return (new InputStreamReader(tmp));
            }
        }

        if (stringVal == null) {
            throw new IllegalStateException("value stream has already been consumed");
        }

        return (new StringReader(stringVal));
    }

    public InputStream asInputStream() {
        if (stream != null) {
            InputStream tmp = stream;

            stream = null;

            return (tmp);
        }

        if (stringVal == null) {
            throw new IllegalStateException("value stream has already been consumed");
        }

        return (IOHelper.newUtf8Stream(stringVal));
    }

    public String asString() {
        if (stringVal != null) {
            return stringVal;
        }

        if (stream == null) {
            throw new IllegalStateException("value stream has already been consumed");
        }

        try {
            stringVal = IOHelper.literalStringFromStream(stream);
        } catch (IOException e) {
            throw new RuntimeException("Could not buffer value as string", e);
        } finally {
            stream = null;
        }

        return (stringVal);
    }
}
