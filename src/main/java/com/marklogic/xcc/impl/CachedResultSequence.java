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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import com.marklogic.http.MultipartBuffer;
import com.marklogic.xcc.RequestOptions;
import com.marklogic.xcc.ResultChannelName;
import com.marklogic.xcc.ResultItem;
import com.marklogic.xcc.ResultSequence;
import com.marklogic.xcc.Request;
import com.marklogic.xcc.types.ValueType;
import com.marklogic.xcc.types.XdmItem;
import com.marklogic.xcc.types.impl.SequenceImpl;
import com.marklogic.xcc.exceptions.RequestException;

public class CachedResultSequence extends AbstractResultSequence {
    private final ArrayList<ResultItem> items = new ArrayList<ResultItem>();
    private boolean closed = false;
    private int cursor = -1;
    private final ResultSequence primary;
    private long totalBytesRead;
    
    // ------------------------------------------------------

    protected CachedResultSequence(ResultSequence primary) {
        // empty sequence constructor used to create empty ResultSequences
        super(((AbstractResultSequence)primary).getRequest());
        this.primary = primary;
    }

    public CachedResultSequence(Request request, MultipartBuffer multipartBuffer, RequestOptions options) throws RequestException, IOException {
        super(request);
        primary = this;

        int index = 0;

        while ((sequencePart != null) || multipartBuffer.hasNext()) {
            ResultItem item = instantiateResultItem(multipartBuffer, index, options);

            item.cache();
            items.add(item);
            index++;
        }
        
        totalBytesRead = multipartBuffer.getTotalBytesRead();
    }

    public long getTotalBytesRead() {
        return totalBytesRead;
    }

    // ------------------------------------------------------

    public int size() {
        return items.size();
    }

    public boolean isCached() {
        return !closed;
    }

    public void close() {
        items.clear(); // let them be GC'ed
        cursor = -1;
        closed = true;
    }

    public boolean isClosed() {
        return closed;
    }

    public boolean hasNext() {
        if (closed) {
            return false;
        }

        if (size() == 0) {
            return false;
        }

        return (cursor + 1) < size();
    }

    public ResultItem next() {
        assertNotClosed();

        if (cursor >= (size() - 1)) {
            cursor = size();
            return null;
        }

        cursor++;

        return items.get(cursor);
    }

    public ResultItem current() {
        assertNotClosed();

        if ((cursor == -1) || (cursor == size())) {
            throw new IllegalStateException("Cursor is not valid");
        }

        return items.get(cursor);
    }

    public ResultItem resultItemAt(int index) {
        assertNotClosed();

        if ((index < 0) || (index >= size())) {
            throw new IllegalArgumentException("Index out of range: size=" + size() + ", requested=" + index);
        }

        return items.get(index);
    }

    public void rewind() {
        assertNotClosed();

        cursor = -1;
    }

    public Iterator<ResultItem> iterator() {
        assertNotClosed();

        return Collections.unmodifiableList(items).iterator();
    }

    public ResultSequence toCached() {
        assertNotClosed();

        return this;
    }

    public ResultSequence getChannel(ResultChannelName channel) {
        assertNotClosed();

        if (channel == ResultChannelName.PRIMARY) {
            return (primary);
        }

        // Nothing else defined yet
        return new EmptyResultSequence(this);
    }

    public boolean isEmpty() {
        return (items.size() == 0);
    }

    public ResultItem[] toResultItemArray() {
        assertNotClosed();

        ResultItem[] array = new ResultItem[size()];

        items.toArray(array);

        return array;
    }

    public XdmItem[] toArray() {
        ResultItem[] resultItems = toResultItemArray();
        XdmItem[] array = new XdmItem[resultItems.length];

        for (int i = 0; i < resultItems.length; i++) {
            array[i] = resultItems[i].getItem();
        }

        return array;
    }

    public XdmItem itemAt(int index) {
        return (resultItemAt(index).getItem());
    }

    public ValueType getValueType() {
        return ValueType.SEQUENCE;
    }

    public String asString(String separator) {
        return SequenceImpl.asStringConcatenation(this, separator);
    }

    public String asString() {
        return (asString("\n"));
    }

    public String[] asStrings() {
        return SequenceImpl.asStringArray(this);
    }

    // -----------------------------------------------------------

    @Override
    public String toString() {
        return "CachedResultSequence: size=" + size() + ", closed=" + isClosed() + ", cursor=" + cursor;
    }

    // -----------------------------------------------------------

    private void assertNotClosed() {
        if (closed) {
            throw new IllegalStateException("ResultSequence is closed");
        }
    }
}
