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

import com.marklogic.xcc.Request;
import com.marklogic.xcc.types.XdmVariable;

/**
 * A server exception resulting from an error encountered while evaluating a 
 * query. Note that some such exceptions are retryable.
 * 
 * @see #isRetryable()
 * @see RetryableQueryException
 */
public abstract class QueryException extends RequestServerException {
    
    private static final long serialVersionUID = 2424928147667627804L;
    final String code;
    final String w3cCode;
    final String formatString;
    final String expr;
    final boolean retryable;
    final String[] data;
    final transient QueryStackFrame[] stack;
    
    /**
     * Constructs an XQueryException
     * 
     * @param request
     *            The {@link com.marklogic.xcc.Request} object to which this exception applies
     * @param code
     *            the symbolic exception code
     * @param w3cCode
     *            the exception code defined by the XQuery spec, if any
     * @param message
     *            the exception message
     * @param formatString
     *            the exception format string
     * @param expr
     *            The expression that caused the exception, if applicable
     * @param retryable
     *            retrying the operation may succeed
     * @param data
     *            the exception data
     * @param stack
     *            the xquery evaluator stack trace
     */
    public QueryException(Request request, String code, String w3cCode, 
            String message, String formatString, String expr, 
            boolean retryable, String[] data, QueryStackFrame[] stack) {
        super(((message == null) || message.length() == 0) ? code : message, request);

        this.code = code;
        this.w3cCode = w3cCode;
        this.formatString = formatString;
        this.expr = expr;
        this.retryable = retryable;
        this.data = data.clone();
        this.stack = stack.clone();
    }
    
    /**
     * Returns the server error code for this XQuery exception.
     * 
     * @return A string that is a server error code, such as XDMP-FOO.
     */
    public String getCode() {
        return code;
    }

    /**
     * Returns the error code defined by the WC3 XQuery spec that corresponds to this exception, if
     * any.
     * 
     * @return A string that is the W3C error code, such as err:BLAH123, if one is defined, else
     *         null.
     * @since 4.0
     */
    public String getW3CCode() {
        return w3cCode;
    }

    public String getFormatString() {
        return formatString;
    }

    /**
     * <p>
     * Returns true if the server indicates that this request might succeeed if
     * resubmitted.
     * </p>
     * <p>
     * Note that if method returns true, then this exception will also be an 
     * instance of
     * {@link RetryableXQueryException}.
     * </p>
     * <p>
     * Note also that retryable exceptions will be automatically resubmitted if
     * the {@link com.marklogic.xcc.RequestOptions} associated with the request 
     * indicate to do so.
     * </p>
     * 
     * @return true if the request is retryable, false if the request cannot be
     * processed as submitted.
     */
    public boolean isRetryable() {
        return retryable;
    }

    public String[] getData() {
        return data.clone();
    }
    

    /**
     * Get an array of {@link QueryStackFrame} objects that represent the query
     * stack frame returned by the server.
     * 
     * @return An array (possibly zero length) of XQuery evaluator stack frames.
     */
    public QueryStackFrame[] getStack() {
        return stack.clone();
    }
    
    /**
     * Returns a string representation (possibly multiple lines) of the error 
     * message and stack frame.
     * 
     * @return A string representation of this Query exception.
     */
    @Override
    public String toString() {
        String nl = System.getProperty("line.separator");
        StringBuffer sb = new StringBuffer(getClass().getName());
        sb.append(": ");

        if (formatString != null && !formatString.equals("")) {
            sb.append(formatString);
        } else {
            sb.append(code);
            for (int i = 0; i < data.length; ++i) {
                sb.append(" ");
                sb.append(data[i]);
            }
        }
        
        appendRequestInfo(sb);

        if (stack != null) {
            for (int i = 0; i < stack.length; ++i) {
                QueryStackFrame frame = stack[i];
                boolean b = false;
                String uri = frame.getUri();

                if (uri != null && uri.length() > 0) {
                    b = true;
                    sb.append(nl);
                    sb.append("in ");
                    sb.append(uri);
                }

                int line = frame.getLineNumber();

                if (line != 0) {
                    if (b) {
                        sb.append(", ");
                    } else {
                        sb.append(nl);
                        b = true;
                    }
                    sb.append("on line ");
                    sb.append(line);
                }

                if ((expr != null) && (expr.length() != 0)) {
                    sb.append(nl);
                    sb.append("expr: ");
                    sb.append(expr);
                }

                String operation = frame.getOperation();

                if (operation != null && operation.length() > 0) {
                    if (b) {
                        sb.append(',');
                    } else {
                        b = true;
                    }
                    sb.append(nl);
                    sb.append("in ");
                    sb.append(operation);
                }

                XdmVariable[] variables = frame.getVariables();

                if (variables != null) {

                    for (int j = 0; j < variables.length; ++j) {
                        XdmVariable variable = variables[j];
                        String name = variable.getName().getLocalname();
                        String value = variable.getValue().asString();

                        if (name != null && name.length() > 0 && 
                            value != null && value.length() > 0) {
                            sb.append(nl);
                            sb.append("  $");
                            sb.append(name);
                            sb.append(" = ");
                            sb.append(value);
                        }
                    }

                    String contextItem = frame.getContextItem();

                    if (contextItem != null && contextItem.length() > 0) {
                        sb.append(nl);
                        sb.append("  context-item() = ");
                        sb.append(contextItem);
                    }

                    int contextPosition = frame.getContextPosition();

                    if (contextPosition != 0) {
                        sb.append(nl);
                        sb.append("  context-position() = ");
                        sb.append(contextPosition);
                    }
                }
            }
        }
        
        return sb.toString();
    }
}
