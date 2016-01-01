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
package com.marklogic.xcc;

/**
 * A specialization of {@link Request} which contains an ad-hoc query (XQuery code as a literal
 * String) to be submitted and evaluated by the MarkLogic Server.
 */
public interface AdhocQuery extends Request {
    /**
     * Replace the XQuery code to be submitted and evaluated with this {@link Request}.
     * 
     * @param query
     */
    void setQuery(String query);

    /**
     * Returns the currently set ad-hoc XQuery string.
     * 
     * @return A String which is the ad-hoc query to run when this {@link Request} is next passed to
     *         {@link Session#submitRequest(Request)}.
     */
    String getQuery();
}
