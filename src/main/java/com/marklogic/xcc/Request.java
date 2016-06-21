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

import com.marklogic.xcc.types.ValueType;
import com.marklogic.xcc.types.XName;
import com.marklogic.xcc.types.XdmValue;
import com.marklogic.xcc.types.XdmVariable;

/**
 * Base Request interface that contains methods common to all requests. Methods defined here manage
 * request options and variable binding.
 */
public interface Request {
    /**
     * The session which created this Request. Request objects are created by factory methods on
     * {@link Session} and their implementations are interdependent.
     * 
     * @return A {@link Session} reference.
     */
    Session getSession();

    /**
     * Associate the given {@link RequestOptions} object with this Request.
     * 
     * @param options
     *            An {@link RequestOptions} instance or null to use default values.
     */
    void setOptions(RequestOptions options);

    /**
     * Returns the currently set {@link RequestOptions} object.
     * 
     * @return An instance of {@link RequestOptions} or null if none is currently in effect.
     */
    RequestOptions getOptions();

    /**
     * <p>
     * Returns a {@link RequestOptions} instance that reflects the execution options that will be
     * applied to this request. This may be a blend of the option values set on the Request and
     * those set on the {@link Session}. This method will always return a newly created, non-null
     * result.
     * </p>
     * <p>
     * The object returned is <strong>NOT</strong> the {@link RequestOptions} object associated with
     * either the Request or the {@link Session}. Making changes to the returned
     * {@link RequestOptions} object will not affect subsequent submissions of this Request.
     * </p>
     * <p>
     * Although it's possible to modify and pass the returned object back to
     * {@link #setOptions(RequestOptions)}, this is not recommended. In general, you should create
     * your own instance of {@link RequestOptions} and set only those options you explictly want to
     * override. When submitted, the options set on the Request are merged with those, if any, set
     * for the {@link Session}.
     * </p>
     * <p>
     * This method mainly used internally. It can be used to determine which values will be
     * submitted with a request. Most client code will not need this method.
     * </p>
     * 
     * @return A newly created instance of {@link RequestOptions}.
     */
    RequestOptions getEffectiveOptions();

    /**
     * Associate the given {@link com.marklogic.xcc.types.XdmVariable} with this Request. If another
     * variable with the same name is already set, it is replaced. When an invocation of
     * {@link Session#submitRequest(Request)} is made, all currently set variables are sent with the
     * request and defined as XQuery external variables when the query runs in the server.
     * 
     * @param variable
     *            A {@link com.marklogic.xcc.types.XdmVariable} instance to be associated with this
     *            Request. If another variable with the same name (
     *            {@link com.marklogic.xcc.types.XName}) is already bound, it will be replaced with
     *            this one. Note that {@link com.marklogic.xcc.types.XName} instances with the same
     *            namespace/local name values are considered to be equal.
     * @throws com.marklogic.xcc.exceptions.UnimplementedFeatureException
     *             If the variable is not a type that can be passed with the Request, 
     *             e.g. XdmSequence is not a supported type for external variables.
     */
    void setVariable(XdmVariable variable);

    /**
     * <p>
     * Convenience method that creates a new {@link XdmVariable}, binds it to this Request and then
     * returns the new {@link XdmVariable} object. This method is equivalent to:
     * </p>
     * <p>
     * <code><pre>
     * XdmVariable temp = ValueFactory.newVariable (xname, value);
     * request.setVariable (temp);
     * return temp;
     * </pre></code>
     * </p>
     * 
     * @param xname
     *            An instance of {@link XName}, which defines a namespace (optional) and a
     *            localname.
     * @param value
     *            An instance of {@link XdmValue} which will be the value of the {@link XdmVariable}
     *            .
     * @return The newly construct {@link XdmVariable} instance.
     */
    XdmVariable setNewVariable(XName xname, XdmValue value);

    /**
     * <p>
     * Convenience method that creates a new {@link XdmVariable}, binds it to this Request and then
     * returns the new {@link XdmVariable} object. This method is equivalent to:
     * </p>
     * <p>
     * <code><pre>
     * XdmVariable temp = ValueFactory.newVariable (
     *     new XName (namespace, localname),
     *     ValueFactory.newValue (type, value));
     * request.setVariable (temp);
     * return temp;
     * </pre></code>
     * </p>
     * 
     * @param namespace
     *            A namespace String, or null for the default namespace.
     * @param localname
     *            The local name as a String.
     * @param type
     *            An instance of {@link ValueType} that indicates which specific subclass of
     *            {@link com.marklogic.xcc.types.XdmValue} to instantiate.
     * @param value
     *            An object that contains the value. The concrete type that should be passed is
     *            dependent on the {@link ValueType} instance provided as the "type" parameter.
     * @return An XdmVariable instance.
     */
    XdmVariable setNewVariable(String namespace, String localname, ValueType type, Object value);

    /**
     * <p>
     * Convenience method that creates a new {@link XdmVariable}, binds it to this Request and then
     * returns the new {@link XdmVariable}. This method is equivalent to:
     * </p>
     * <p>
     * <code><pre>
     * XdmVariable temp = ValueFactory.newVariable (
     *     new XName (null, localname),
     *     ValueFactory.newValue (type, value));
     * request.setVariable (temp);
     * return temp;
     * </pre></code>
     * </p>
     * 
     * @param localname
     *            The local name as a String.
     * @param type
     *            An instance of {@link ValueType} that indicates which specific subclass of
     *            {@link com.marklogic.xcc.types.XdmValue} to instantiate.
     * @param value
     *            An object that contains the value. The concrete type that should be passed is
     *            dependent on the {@link ValueType} instance provided as the "type" parameter.
     * @return An XdmVariable instance.
     */
    XdmVariable setNewVariable(String localname, ValueType type, Object value);

    /**
     * <p>
     * Convenience method that creates a new {@link com.marklogic.xcc.types.XSString}, binds it to
     * this Request and then returns the new {@link com.marklogic.xcc.types.XSString} instance. This
     * method is equivalent to:
     * </p>
     * <p>
     * <code><pre>
     * XdmVariable temp = ValueFactory.newVariable (
     *     new XName (namespace, localname),
     *     ValueFactory.newValue (ValueType.XS_STRING, value));
     * request.setVariable (temp);
     * return temp;
     * </pre></code>
     * </p>
     * 
     * @param namespace
     *            A namespace String, or null for the default namespace.
     * @param localname
     *            The local name as a String.
     * @param value
     *            A String that contains the value.
     * @return An instance of {@link com.marklogic.xcc.types.XSString}
     */
    XdmVariable setNewStringVariable(String namespace, String localname, String value);

    /**
     * <p>
     * Convenience method that creates a new {@link com.marklogic.xcc.types.XSString}, binds it to
     * this Request and then returns the new {@link com.marklogic.xcc.types.XSString} instance. This
     * method is equivalent to:
     * </p>
     * <p>
     * <code><pre>request.setNewStringVariable (null, localname, value)
	 * </pre></code>
     * </p>
     * 
     * @param localname
     *            The local name as a String.
     * @param value
     *            A String that contains the value.
     * @return An instance of {@link com.marklogic.xcc.types.XSString}
     * @see #setNewStringVariable(String, String, String)
     */
    XdmVariable setNewStringVariable(String localname, String value);

    /**
     * <p>
     * Convenience method that creates a new {@link com.marklogic.xcc.types.XSInteger}, binds it to
     * this Request and then returns the new {@link com.marklogic.xcc.types.XSInteger} instance.
     * This method is equivalent to:
     * </p>
     * <p>
     * <code><pre>
     * XdmVariable temp = ValueFactory.newVariable (
     *     new XName (namespace, localname),
     *     ValueFactory.newValue (ValueType.XS_INTEGER, value));
     * request.setVariable (temp);
     * return temp;
     * </pre></code>
     * </p>
     * 
     * @param namespace
     *            A namespace String, or null for the default namespace.
     * @param localname
     *            The local name as a String.
     * @param value
     *            A long that contains the value. Note that XQuery integers may contain much larger
     *            values that Java ints. If you need to specify a value larger than can be expressed
     *            by a long, use
     *            {@link ValueFactory#newValue(com.marklogic.xcc.types.ValueType, Object)} and pass
     *            a {@link java.math.BigInteger} object as the value.
     * @return An instance of {@link com.marklogic.xcc.types.XSInteger}
     */
    XdmVariable setNewIntegerVariable(String namespace, String localname, long value);

    /**
     * <p>
     * Convenience method that creates a new {@link com.marklogic.xcc.types.XSInteger}, binds it to
     * this Request and then returns the new {@link com.marklogic.xcc.types.XSInteger} instance.
     * This method is equivalent to:
     * </p>
     * <p>
     * <code><pre>request.setNewIntegerVariable (null, localname, value)
	 * </pre></code>
     * </p>
     * 
     * @param localname
     *            The local name as a String.
     * @param value
     *            A long that contains the value.
     * @return An instance of {@link com.marklogic.xcc.types.XSInteger}
     * @see #setNewIntegerVariable(String, String, long)
     */
    XdmVariable setNewIntegerVariable(String localname, long value);

    /**
     * Remove the given variable from this Request context. The variable and its value are still
     * valid and may be reassociated with the session by passing it to {@link #setVariable}.
     * 
     * @param variable
     *            A {@link XdmVariable} instance to be diassociated from this Request.
     */
    void clearVariable(XdmVariable variable);

    /**
     * Remove any variables set in this Request.
     */
    void clearVariables();

    /**
     * Return an array (possibly zero length) of all the {@link XdmVariable} objects currently set
     * on this Request.
     * 
     * @return An array of {@link XdmVariable} objects.
     */
    XdmVariable[] getVariables();
    
    /**
     * Get starting position of results for this request.
     * 
     * @return starting position of results for this request.
     */
    long getPosition();
    
    /**
     * Set starting position of results for this request.
     * 
     * @param position starting position.
     */
    void setPosition(long position);

    /**
     * Get count of result items for this request.
     * 
     * @return count
     */
    long getCount();

    /**
     * Set count of result items for this request.
     * 
     * @param count count.
     */
    void setCount(long count);
}
