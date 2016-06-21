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
package com.marklogic.xcc;

import java.util.Iterator;

import com.marklogic.xcc.types.XdmItem;
import com.marklogic.xcc.types.XdmSequence;

/**
 * <p>
 * A sequence of {@link ResultItem} values as returned from a query or module execution. A
 * ResultSequence instance is stateful, it holds an internal positional index (cursor) which is
 * updated on successive accesses. It is therefore not recommended that ResultSequence objects be
 * accessed concurrently by multiple threads.
 * </p>
 * <p>
 * ResultSequence instances can operate in two modes: cached and non-cached. If a ResultSequence is
 * cached (default) then all {@link com.marklogic.xcc.types.XdmItem}s have been read and buffered.
 * If non-cached, then items may only be accessed sequentially and some values may only be fetched
 * once if accessed as an {@link java.io.InputStream} or {@link java.io.Reader}.
 * </p>
 * <p>
 * Cached ResultSequences need not be closed. Upon return, they no longer tie up any connection
 * resources. However, closing a cached ResultSequence will invalidate it and prevent further access
 * to its contents.
 * </p>
 * <p>
 * Streaming (non-cached) ResultSequences should always be promptly closed. They hold a server
 * connection until closed. If not closed, connections may be tied up indefinitely. Additionally,
 * you should always consume the ResultSequence in a timely manner, because the server may close the
 * connection if the (server-side configurable) request tiemout expires before the data is read out.
 * <p>
 * </p>
 * In general, it is preferable to use cached ResultSequences (this is the default) unless you have
 * reason to believe the amount of data returned may be too large to be fully buffered in memory.
 * </p>
 */
public interface ResultSequence extends XdmSequence<ResultItem> {
    /**
     * <p>
     * Returns the number of {@link ResultItem}s, if known, in this ResultSequence. For streaming
     * (non-cached) ResultSequences, {@link ResultItem}s are processed sequentially from the
     * communication channel and so the size of the full sequence is not known during iteration.
     * </p>
     * 
     * @return The size, if this ResultSequence is cached, otherwise -1.
     */
    int size();

    /**
     * <p>
     * Indicates whether this is a cached (detached) ResultSequence. A cached ResultSequence has
     * fully buffered all the {@link ResultItem} data values and no longer depends on an active
     * server connection. A cached ResultSequence may be accessed repeatedly and/or randomly.
     * </p>
     * <p>
     * Cached ResultSequence objects do not need to be closed because they no longer have any
     * reference to a server connection. But if they are closed (by invoking {@link #close()}, then
     * the buffered data values are released and it is no longer usable.
     * </p>
     * 
     * @return True if the entire ResultSequence is cached. If this ResultSequence is closed, then
     *         false is returned.
     */
    boolean isCached();

    /**
     * Release any resources being held by this ResultSequence. If cached, this is a no-op an the
     * cached {@link ResultItem}s are retained (let the ResultSequence go out of scope to cause the
     * cached data to be reclaimed). If not cached, this ResultSequence will be invalidated and its
     * {@link ResultItem} members will no longer be accessible.
     */
    void close();

    /**
     * Indicates whether this ResultSequence is closed.
     * 
     * @return true if closed, false if open.
     */
    boolean isClosed();

    /**
     * Returns true if this sequence contains another item beyond the currently active one.
     * Initially, the result sequence is positioned before the first item, if any, and this method
     * will return false if the sequence is empty. Note that if the current item is large (node,
     * binary, text) and has not yet been fully consumed by the client, it's value may be flushed
     * and lost as the result stream is positioned to the next item.
     * 
     * @return True if at least one more item exists in this ResultSequence. If closed, this method
     *         always returns false.
     */
    boolean hasNext();

    /**
     * <p>
     * Advance the logical cursor to the next {@link ResultItem} in this ResultSequence and return
     * that item. The logical cursor is initially positioned before the first item in the sequence.
     * </p>
     * <p>
     * Unlike {@link java.util.Iterator#next()}, this method returns null when there is no next
     * item.
     * </p>
     * 
     * @return The next {@link ResultItem} in this sequence, or null if the end of the sequence has
     *         been reached. Note that the sequence may be empty and this method may return null on
     *         the first call.
     * @throws IllegalStateException
     *             If this ResultSequence is closed.
     */
    ResultItem next();

    /**
     * <p>
     * Fetch the current {@link ResultItem} object in the ResultSequence. The method
     * {@link com.marklogic.xcc.ResultItem#isFetchable()} indicates whether the value for this item
     * is available. Simple data values (numbers, dates, durations, etc) are cached and may always
     * be re-fetched.
     * </p>
     * <p>
     * Large data values (nodes, text, etc) that are read as a stream or a reader are not guaranteed
     * to be fetchable more than once.
     * </p>
     * 
     * @return The currently active {@link ResultItem} in this sequence, or null if the cursor is
     *         not currently positioned on an item. This will be the case before the first call to
     *         {@link #next()} and after {@link #next()} returns null.
     * @throws IllegalStateException
     *             If this method is called before the first call to {@link #next()} or after
     *             {@link #next()} returns null or if closed.
     * @see com.marklogic.xcc.ResultItem#isFetchable()
     * @see #isCached()
     * @see com.marklogic.xcc.ResultItem#isCached()
     */
    ResultItem current();

    /**
     * <p>
     * Returns the {@link ResultItem} from this ResultSequence, if possible, at the given positional
     * index. Accessing {@link ResultItem}s randomly has restrictions if {@link #isCached()} returns
     * true.
     * </p>
     * <p>
     * For streaming ResultSequences, if index is equal to the current position, then the current
     * {@link ResultItem} is returned. If index is less than the current position, then an exception
     * is thrown. If index is greater than the current position, then items in the sequence are read
     * and discarded until the requested position is achieved. If the requested item is found, it is
     * returned. If the end of the sequence is encountered while trying to seek to the requested
     * position, an exception is thrown.
     * </p>
     * 
     * @param index
     *            The position (zero-based) of the {@link ResultItem} to return.
     * @return The {@link ResultItem} at the given index.
     * @throws IllegalArgumentException
     *             If index is negative or greater than or equal to {@link #size}.
     * @throws IllegalStateException
     *             If this ResultSequence is not cached ({@link #isCached()} returns false) and the
     *             index does not match the current cursor value, or if closed.
     */
    ResultItem resultItemAt(int index);

    /**
     * Returns the {@link XdmItem} wrapped by the {@link ResultItem} at the given index.
     * 
     * @param index
     *            The position (zero-based) of the {@link ResultItem} to return.
     * @return The underlying {@link XdmItem} for the {@link ResultItem} at the requested index.
     * @throws IllegalArgumentException
     *             If index is negative or greater than or equal to {@link #size}.
     * @throws IllegalStateException
     *             If this ResultSequence is not cached ({@link #isCached()} returns false) and the
     *             index xdoes not match the current cursor value, or if closed.
     */
    XdmItem itemAt(int index);

    /**
     * Reset the internal positional index (cursor) of this ResultSequence to before the first
     * {@link ResultItem} in the sequence. This operation is only supported if {@link #isCached()}
     * returns true.
     * 
     * @throws IllegalStateException
     *             If called on a non-cached (streaming) ResultSequence ({@link #isCached()} returns
     *             false).
     * @throws IllegalStateException
     *             If this ResultSequence streaming or is closed.
     */
    void rewind();

    /**
     * Returns a java.util.Iterator instance that may be used to iterate over this result sequence.
     * Each object returned by the Iterator is an instance of {@link ResultItem}.
     * 
     * @return An Iterator instance for this ResultSequence.
     * @throws IllegalStateException
     *             If this ResultSequence is closed.
     */
    Iterator<ResultItem> iterator();

    /**
     * Produce a cached version of this ResultSequence. If the provided ResultSequence is already
     * cached ({@link #isCached()} returns true), then nothing is done and this instance returns
     * itself. Otherwise, a new, cached ResultSequence instance is created and populated with the
     * {@link com.marklogic.xcc.types.XdmItem}s in the sequence. If the non-cached input
     * ResultSequence has been partially iterated, only the remaining
     * {@link com.marklogic.xcc.types.XdmItem}s will be cached. If the current item is not fetchable
     * ({@link com.marklogic.xcc.ResultItem#isFetchable()} returns false), it will be ignored.
     * 
     * @return A Cached ResultSequence.
     * @throws IllegalStateException
     *             If this ResultSequence is closed.
     * @see #isCached()
     */
    ResultSequence toCached();

    /**
     * <p>
     * This method is identical to the superclass method {@link #toArray()}, but returns an array
     * typed as {@link ResultItem}.
     * </p>
     * <p>
     * Invoking {@link #toArray()} on a ResultSequence actually invokes this method. If the
     * ResultSequence is streaming ({@link #isCached()} returns false), then it is consumed and
     * closed after building the returned array. Note also that {@link #asStrings()} when called on
     * a ResultSequence will call this method internally, which will also result in the object being
     * closed automatically.
     * </p>
     * <p>
     * Note that for a streaming ResultSequence, all items will be loaded into memory to build the
     * array. If very large items (node(), text(), etc) are in the stream, it's possible that there
     * may not be enough memory to buffer everything. If the sequence contains too much data to
     * buffer, iterate over each item and use {@link com.marklogic.xcc.ResultItem#asInputStream()}
     * to read each item in turn as an {@link java.io.InputStream}.
     * </p>
     * 
     * @return An array of {@link ResultItem} instances, all of which will be cached.
     * @throws IllegalStateException
     *             If this ResultSequence is closed.
     * @throws com.marklogic.xcc.exceptions.StreamingResultException
     *             If an IOException ocurrs while buffering a {@link ResultItem}
     */
    ResultItem[] toResultItemArray();

    /**
     * Return the {@link ResultSequence} for an alternate {@link ResultChannelName}. The
     * {@link ResultSequence} returned by {@link Session#submitRequest(Request)} is the sequence of
     * {@link com.marklogic.xcc.types.XdmItem}s representing the XQuery result. But there may be
     * alternate channels (each comprising a {@link ResultSequence} instance) associated with the
     * result. This method returns the sequence for the given channel. The {@link ResultSequence}
     * returned by {@link Session#submitRequest(Request)} is always
     * {@link ResultChannelName#PRIMARY}. A {@link ResultSequence} instance is always returned by
     * this method, though it may be empty.
     * 
     * @param channel
     *            An instance of {@link ResultChannelName} that indicates which channel to return.
     * @return An instance of {@link ResultSequence}, possibly empty.
     * @throws IllegalStateException
     *             If this ResultSequence is closed.
     */
    ResultSequence getChannel(ResultChannelName channel);

    /**
     * Return a textual description of this ResultSequence object, <strong>NOT</strong> the value of
     * the items in the sequence. Use the {@link #asString()} or {@link #asStrings()} methods to
     * obtain {@link String} representations of the item values.
     * 
     * @return A textual description of this object, appropriate for use in a debug or log message.
     * @see #asString()
     * @see #asStrings()
     */
    String toString();
}
