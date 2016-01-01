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

import java.util.Iterator;

/**
 * A {@link XdmValue} which is a sequence of Items.
 */
public interface XdmSequence<I extends XdmItem> extends XdmValue {
    /**
     * Returns the size of this XdmSequence.
     * 
     * @return The number of Items (possibly zero) in this sequence.
     */
    int size();

    /**
     * Indicates whether this XdmSequence is empty.
     * 
     * @return true if size() == 0.
     */
    boolean isEmpty();

    /**
     * Construct an array of {@link XdmItem}s from this {@link XdmSequence}.
     * 
     * @return An array of {@link XdmItem}, possibly zero-length.
     */
    XdmItem[] toArray();

    /**
     * Return the {@link XdmItem} at the given position from this XdmSequence.
     * 
     * @param index
     *            The index of the {@link XdmItem} (zero-based) to return.
     * @return An {@link XdmItem} instance.
     * @throws IllegalArgumentException
     *             If index is negative or is greater than or equal to {@link #size()}.
     */
    XdmItem itemAt(int index);

    /**
     * A java.util.Iterator instance that iterates over the items in this XdmSequence.
     * 
     * @return An Iterator over the {@link XdmItem} instances in this XdmSequence.
     */
    Iterator<I> iterator();

    /**
     * Returns a String comprised of {@link com.marklogic.xcc.types.XdmValue#asString()} value of
     * each item in the sequence with the given separator string between each. If the sequence is
     * empty ({@link #isEmpty()} is true) then the empty string is returned.
     * 
     * @param separator
     *            A separator string, such as "\n", to be inserted between the
     *            {@link com.marklogic.xcc.types.XdmValue#asString()} value of each item in the
     *            sequence. A value of null is equivalent to the empty string which causes all items
     *            to be concatenated with no separator.
     * @return A string representation of the sequence.
     */
    String asString(String separator);

    /**
     * This method is equivalent to <code>asString ("\n")</code>.
     * 
     * @return A string representation of the sequence with a newline separator.
     */
    String asString();

    /**
     * Returns an array of {@link String}s, each of which is the value returned by the
     * {@link com.marklogic.xcc.types.XdmItem#asString()} method for each item in the sequence.
     * 
     * @return An array of {@link String}s.
     */
    String[] asStrings();

    /**
     * Return a textual description of this XdmSequence object, <strong>NOT</strong> the value of
     * the items in the sequence. Use the {@link #asString()} or {@link #asStrings()} methods to
     * obtain {@link String} representations of the item values.
     * 
     * @return A textual description of this object, appropriate for use in a debug or log message.
     * @see #asString()
     * @see #asStrings()
     */
    String toString();
}
