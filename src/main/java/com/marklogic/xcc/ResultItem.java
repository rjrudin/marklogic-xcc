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

import com.marklogic.xcc.types.XdmItem;

/**
 * <p>
 * An {@link com.marklogic.xcc.types.XdmItem} that is a member of a {@link ResultSequence}. The
 * values associated with ResultItem instances may be transient. ResultItem wraps an {@link XdmItem}
 * instance and also implements the {@link XdmItem} interface. Invoking the methods of the
 * {@link XdmItem} interface are passed through to the contained instance.
 * </p>
 * <p>
 * Because a {@link ResultSequence} may be streaming, the contained {@link XdmItem} may be have been
 * partially consumed. Use the {@link #isFetchable()} method to determine if it is safe to access
 * the value.
 * </p>
 * 
 * @see com.marklogic.xcc.types
 */
public interface ResultItem extends XdmItem {
	/**
	 * <p>Returns the URI of the document represented by this ResultItem, as returned by xdmp:node-uri($node).</p>
	 * <p>Returns null for non-node items and constructed nodes.</p>
	 */
	String getDocumentURI();
	
	/**
	 * <p>Returns the path of the node represented by this ResultItem, as returned by xdmp:path($node, false()).</p>
	 * <p>Returns null for non-node items.</p>
	 */
	String getNodePath();

	/**
     * Returns the actual {@link XdmItem} value wrapped by this ResultItem. The instance returned
     * may be tested with <code>instanceof</code>.
     * 
     * @return an instance of {@link XdmItem}.
     */
    XdmItem getItem();

    /**
     * The position (zero-based) of this ResultItem in its containing {@link ResultSequence}.
     * 
     * @return This ResultItem's positional index.
     */
    int getIndex();

    /**
     * Indicates if the value of this ResultItem may be fetched. For large values that are streamed,
     * this method will return false after {@link #asInputStream()} or {@link #asReader()} is
     * called.
     * 
     * @return true if the XdmItem may be fetched, false if not. This will always be true if
     *         isCached() is true. It will also always be true immediately after
     *         {@link com.marklogic.xcc.ResultSequence#next()} is called and returns this
     *         {@link XdmItem}.
     */
    boolean isFetchable();

    /**
     * If this item is not already cached, read it fully from the result stream so that it is
     * buffered in memory. If the item is already cached, this is a no-op.
     * 
     * @throws com.marklogic.xcc.exceptions.StreamingResultException
     *             If an IOException ocurrs reading the result data.
     */
    void cache();
}
