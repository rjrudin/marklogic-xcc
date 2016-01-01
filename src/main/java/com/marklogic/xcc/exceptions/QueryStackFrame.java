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
package com.marklogic.xcc.exceptions;

import com.marklogic.xcc.types.XdmVariable;

/**
 * A query stack frame.
 */
public class QueryStackFrame {
    private String uri;
    private int line;
    private String operation;
    private XdmVariable[] variables;
    private String contextItem;
    private int contextPosition;
    private String xqueryVersion;

    /**
     * Construct a stack frame.
     * 
     * @param uri
     *            The URI of the XQuery source.
     * @param line
     *            The line number in the XQuery source.
     * @param operation
     *            The description of the current operation.
     * @param variables
     *            The variable bindings.
     * @param contextItem
     *            The context item or null.
     * @param contextPosition
     *            The context position or 0.
     * @param xqueryVersion
     *            The XQuery version of the module
     */
    public QueryStackFrame(String uri, int line, String operation, XdmVariable[] variables, String contextItem,
            int contextPosition, String xqueryVersion) {
        this.uri = uri;
        this.line = line;
        this.operation = operation;
        this.variables = variables.clone();
        this.contextItem = contextItem;
        this.contextPosition = contextPosition;
        this.xqueryVersion = xqueryVersion;
    }

    // -----------------------------------------------------------

    /**
     * Get the URI of the XQuery source.
     * 
     * @return The URI of the module, if defined.
     */
    public String getUri() {
        return uri;
    }

    /**
     * Get the line number in the XQuery source.
     * 
     * @return The line number for this string
     */
    public int getLineNumber() {
        return line;
    }

    /**
     * Get the description of the current operation.
     * 
     * @return A description of the operation
     */
    public String getOperation() {
        return operation;
    }

    /**
     * Get the variable bindings.
     * 
     * @return The variable bindings
     */
    public XdmVariable[] getVariables() {
        return variables.clone();
    }

    /**
     * Get the context item, or null if there is no context defined.
     * 
     * @return The context item or null
     */
    public String getContextItem() {
        return contextItem;
    }

    /**
     * Get the context position, or 0 if there is no context defined.
     * 
     * @return The context position, or 0
     */
    public int getContextPosition() {
        return contextPosition;
    }

    /**
     * Get the XQuery version for this stack frame
     * 
     * @return The XQuery version of the module
     */
    public String getXQueryVersion() {
        return xqueryVersion;
    }
}
