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

import java.util.logging.Logger;

import com.marklogic.io.IOHelper;
import com.marklogic.xcc.AdhocQuery;
import com.marklogic.xcc.RequestOptions;
import com.marklogic.xcc.Session;

public class AdhocImpl extends RequestImpl implements AdhocQuery {
    private String query;

    public AdhocImpl(Session session, String query, RequestOptions options) {
        super(session, options);

        this.query = query;
    }

    // --------------------------------------------------

    public void setQuery(String query) {
        this.query = query;
    }

    public String getQuery() {
        return query;
    }

    // --------------------------------------------------

    @Override
    String requestVar() {
        String queryLanguage = getEffectiveOptions().getQueryLanguage();
        if ("javascript".equalsIgnoreCase(queryLanguage)) {
            return "javascript";
        }
        return "xquery";
    }

    @Override
    String serverPath() {
        return "/eval";
    }

    @Override
    void urlEncodeXQueryString(StringBuffer sb, Logger logger) {
        IOHelper.urlEncodeToStringBuffer(sb, query);
    }
}
