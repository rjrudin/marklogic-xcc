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
import java.net.URLDecoder;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import com.marklogic.http.MultipartBuffer;
import com.marklogic.xcc.RequestOptions;
import com.marklogic.xcc.ResultItem;
import com.marklogic.xcc.ResultSequence;
import com.marklogic.xcc.Request;
import com.marklogic.xcc.types.XdmItem;
import com.marklogic.xcc.types.XdmNode;
import com.marklogic.xcc.types.impl.ArrayNodeImpl;
import com.marklogic.xcc.types.impl.AttributeImpl;
import com.marklogic.xcc.types.impl.BinaryImpl;
import com.marklogic.xcc.types.impl.BooleanNodeImpl;
import com.marklogic.xcc.types.impl.CommentImpl;
import com.marklogic.xcc.types.impl.CtsBoxImpl;
import com.marklogic.xcc.types.impl.CtsCircleImpl;
import com.marklogic.xcc.types.impl.CtsPointImpl;
import com.marklogic.xcc.types.impl.CtsPolygonImpl;
import com.marklogic.xcc.types.impl.DocumentImpl;
import com.marklogic.xcc.types.impl.ElementImpl;
import com.marklogic.xcc.types.impl.JSArrayImpl;
import com.marklogic.xcc.types.impl.JSObjectImpl;
import com.marklogic.xcc.types.impl.NullNodeImpl;
import com.marklogic.xcc.types.impl.NumberNodeImpl;
import com.marklogic.xcc.types.impl.ObjectNodeImpl;
import com.marklogic.xcc.types.impl.ProcessingInstructionImpl;
import com.marklogic.xcc.types.impl.TextImpl;
import com.marklogic.xcc.types.impl.XsAnyUriImpl;
import com.marklogic.xcc.types.impl.XsBase64BinaryImpl;
import com.marklogic.xcc.types.impl.XsBooleanImpl;
import com.marklogic.xcc.types.impl.XsDateImpl;
import com.marklogic.xcc.types.impl.XsDateTimeImpl;
import com.marklogic.xcc.types.impl.XsDayTimeDurationImpl;
import com.marklogic.xcc.types.impl.XsDecimalImpl;
import com.marklogic.xcc.types.impl.XsDoubleImpl;
import com.marklogic.xcc.types.impl.XsDurationImpl;
import com.marklogic.xcc.types.impl.XsFloatImpl;
import com.marklogic.xcc.types.impl.XsGDayImpl;
import com.marklogic.xcc.types.impl.XsGMonthDayImpl;
import com.marklogic.xcc.types.impl.XsGMonthImpl;
import com.marklogic.xcc.types.impl.XsGYearImpl;
import com.marklogic.xcc.types.impl.XsGYearMonthImpl;
import com.marklogic.xcc.types.impl.XsHexBinaryImpl;
import com.marklogic.xcc.types.impl.XsIntegerImpl;
import com.marklogic.xcc.types.impl.XsQNameImpl;
import com.marklogic.xcc.types.impl.XsStringImpl;
import com.marklogic.xcc.types.impl.XsTimeImpl;
import com.marklogic.xcc.types.impl.XsUntypedAtomicImpl;
import com.marklogic.xcc.types.impl.XsYearMonthDurationImpl;
import com.marklogic.xcc.impl.handlers.ServerErrorParser;
import com.marklogic.xcc.exceptions.RequestException;

public abstract class AbstractResultSequence implements ResultSequence {
    protected static final Set<String> stringConstructedTypes = new HashSet<String>();

    static {
        stringConstructedTypes.add("string");
        stringConstructedTypes.add("integer");
        stringConstructedTypes.add("anyURI");
        stringConstructedTypes.add("QName");
        stringConstructedTypes.add("boolean");
        stringConstructedTypes.add("decimal");
        stringConstructedTypes.add("double");
        stringConstructedTypes.add("float");
        stringConstructedTypes.add("untypedAtomic");
        stringConstructedTypes.add("anySimpleType");
        stringConstructedTypes.add("date");
        stringConstructedTypes.add("dateTime");
        stringConstructedTypes.add("time");
        stringConstructedTypes.add("gDay");
        stringConstructedTypes.add("gMonth");
        stringConstructedTypes.add("gMonthDay");
        stringConstructedTypes.add("gYear");
        stringConstructedTypes.add("gYearMonth");
        stringConstructedTypes.add("duration");
        stringConstructedTypes.add("dayTimeDuration");
        stringConstructedTypes.add("yearMonthDuration");
        stringConstructedTypes.add("base64Binary");
        stringConstructedTypes.add("hexBinary");
        stringConstructedTypes.add("box");
        stringConstructedTypes.add("circle");
        stringConstructedTypes.add("point");
        stringConstructedTypes.add("polygon");
        stringConstructedTypes.add("array");
        stringConstructedTypes.add("map");
        stringConstructedTypes.add("object");
        stringConstructedTypes.add("jsfunction");
    }

    protected Request request;
    protected String sequencePart = null;
    protected int sequencePosition = 0;
    
    protected AbstractResultSequence(Request request) {
        this.request = request;
    }

    Request getRequest() {
        return request;
    }

    // ----------------------------------------------------------------

    abstract public long getTotalBytesRead();

    protected ResultItem instantiateResultItem(MultipartBuffer mbuf, int index, RequestOptions options)
            throws RequestException, IOException {
        String contentType = null;
        if (sequencePart == null) {
            mbuf.next();
            contentType = mbuf.getHeader("content-type");
            if (contentType == null) {
                throw new IllegalStateException("No content-type header in part");
            }
            if (contentType.equals("application/vnd.marklogic.sequence")) {
                sequencePart = mbuf.getBodyAsString();
                sequencePosition = 0;
            }
        }
        XdmItem item = null;
        if (sequencePart == null) {
            item = instantiateXdmItem(contentType, mbuf, options);
        } else {
            TimeZone timezone = options.getTimeZone();
            Locale locale = options.getLocale();

            int i = sequencePosition; 
            while (i < sequencePart.length() && 
                   sequencePart.charAt(i) != ':') {
                ++i;
            }
            if (sequencePart.length() == i) {
                throw new IllegalStateException("Unexpected EOF: " + 
                        sequencePart.substring(sequencePosition));
            }
            String primitive = sequencePart.substring(sequencePosition, i);
            sequencePosition = i+1;
            
            String body;
            
            if (primitive.equals("string")||primitive.equals("untypedAtomic")) {
                i = sequencePosition; 
                while ((i < sequencePart.length()) && 
                        (sequencePart.charAt(i) != ':')) {
                    ++i;
                }
                if (sequencePart.length() == i) {
                    throw new IllegalStateException("Unexpected EOF: " + 
                            sequencePart.substring(sequencePosition));
                }
                int length = Integer.parseInt(
                        sequencePart.substring(sequencePosition, i));
                int offset = i + 1;
                for (int cp = 0, count = 0; 
                     count < length; 
                     offset += Character.charCount(cp), count++) {
                    cp = sequencePart.codePointAt(offset);
                }
                body = sequencePart.substring(i+1, offset); 
                i = offset;
            } else {
                i = sequencePosition; 
                while ((i < sequencePart.length()) && 
                        (sequencePart.charAt(i) != '\n')) {
                    ++i;
                }
                body = sequencePart.subSequence(sequencePosition, i).toString();
            }
            
            if (i < sequencePart.length()) {
                sequencePosition = i+1;
            } else {
                sequencePart = null;
                sequencePosition = 0;
            }

            if (stringConstructedTypes.contains(primitive)) {
                item = (instantiateTypeFromString(primitive, body, timezone, locale));
            } else {
                item = nodeFactory(contentType, primitive, body, options.getCacheResult());
            }
        }

        String uri = mbuf.getHeader("x-uri");
        String path = mbuf.getHeader("x-path");

        if (uri != null) {
            uri = URLDecoder.decode(uri, "UTF-8");
        }
        
        return new ResultItemImpl(item, index, uri, path);
    }

    private XdmItem instantiateXdmItem(String contentType, MultipartBuffer mbuf, RequestOptions options) throws RequestException, IOException {
        TimeZone timezone = options.getTimeZone();
        Locale locale = options.getLocale();

        String error = mbuf.getHeader("x-error");
        if(error != null && error.equals("true")) {
            RequestException ex = ServerErrorParser.makeException(request, mbuf.getBodyAsString());
            ex.setStackTrace((new Exception()).getStackTrace());
            throw ex;
        }

        String primitive = mbuf.getHeader("x-primitive");
        
        if ((primitive == null) || (primitive.length() == 0)) {
            throw new IllegalStateException("Result item has no x-primitive header value");
        }

        if (stringConstructedTypes.contains(primitive)) {
            return (instantiateTypeFromString(primitive, mbuf.getBodyAsString(), timezone, locale));
        }

        return nodeFactory(contentType, primitive, mbuf, options.getCacheResult());
    }

    private XdmNode nodeFactory(String contentType, String type, String body, boolean cache) throws IOException {
        if (type.equals("text()")) {
            return (new TextImpl(body));
        }
        else if (type.equals("node()")) {
            return new ElementImpl(body);
        }
        else {
            throw new IOException("Nodes of type '" + type + "' are not supported in XCC result sequences");
        }
    }
    
    private XdmNode nodeFactory(String contentType, String type, MultipartBuffer mbuf, boolean cache) throws IOException {
        if (cache) {
            if (type.equals("text()"))
                return new TextImpl(mbuf.getBodyAsString());
            if (type.equals("binary()"))
                return new BinaryImpl(mbuf.getBodyStream(), true);
            if (type.equals("document-node()"))
                return new DocumentImpl(mbuf.getBodyAsString());
            if (type.equals("element()"))
                return new ElementImpl(mbuf.getBodyAsString());
            if (type.equals("attribute()"))
                return new AttributeImpl(mbuf.getHeader("x-attr"), mbuf.getBodyAsString());
            if (type.equals("processing-instruction()"))
                return new ProcessingInstructionImpl(mbuf.getBodyAsString());
            if (type.equals("comment()"))
                return new CommentImpl(mbuf.getBodyAsString());
            // reproduce pre-5.0 behavior for pre-5.0 server
            if (type.equals("node()")) 
                return new ElementImpl(mbuf.getBodyAsString());
            if (type.equals("object-node()"))
                return new ObjectNodeImpl(mbuf.getBodyAsString());
            if (type.equals("array-node()"))
                return new ArrayNodeImpl(mbuf.getBodyAsString());
            if (type.equals("null-node()"))
                return new NullNodeImpl(mbuf.getBodyAsString());
            if (type.equals("boolean-node()"))
                return new BooleanNodeImpl(mbuf.getBodyAsString());
            if (type.equals("number-node()"))
                return new NumberNodeImpl(mbuf.getBodyAsString());
        } else {
            if (type.equals("text()"))
                return new TextImpl(mbuf.getBodyStream());
            if (type.equals("binary()"))
                return new BinaryImpl(mbuf.getBodyStream(), false);
            if (type.equals("document-node()"))
                return new DocumentImpl(mbuf.getBodyStream());
            if (type.equals("element()"))
                return new ElementImpl(mbuf.getBodyStream());
            if (type.equals("attribute()"))
                return new AttributeImpl(mbuf.getHeader("x-attr"), mbuf.getBodyStream());
            if (type.equals("processing-instruction()"))
                return new ProcessingInstructionImpl(mbuf.getBodyStream());
            if (type.equals("comment()"))
                return new CommentImpl(mbuf.getBodyStream());
            // reproduce pre-5.0 behavior for pre-5.0 server
            if (type.equals("node()")) 
                return new ElementImpl(mbuf.getBodyStream());
            if (type.equals("object-node()"))
                return new ObjectNodeImpl(mbuf.getBodyStream());
            if (type.equals("array-node()"))
                return new ArrayNodeImpl(mbuf.getBodyStream());
            if (type.equals("null-node()"))
                return new NullNodeImpl(mbuf.getBodyStream());
            if (type.equals("boolean-node()"))
                return new BooleanNodeImpl(mbuf.getBodyStream());
            if (type.equals("number-node()"))
                return new NumberNodeImpl(mbuf.getBodyStream());
        }

        throw new IOException("Nodes of type '" + type + "' are not supported in XCC result sequences");
    }

    private XdmItem instantiateTypeFromString(String typeName, String bodyString, TimeZone timezone, Locale locale)
            throws IOException {
        if (typeName.equals("string")) {
            return (new XsStringImpl(bodyString));
        }
        if (typeName.equals("integer")) {
            return (new XsIntegerImpl(bodyString));
        }
        if (typeName.equals("anyURI")) {
            return (new XsAnyUriImpl(bodyString));
        }
        if (typeName.equals("QName")) {
            return (new XsQNameImpl(bodyString));
        }
        if (typeName.equals("boolean")) {
            return (new XsBooleanImpl(bodyString));
        }
        if (typeName.equals("decimal")) {
            return (new XsDecimalImpl(bodyString));
        }
        if (typeName.equals("double")) {
            return (new XsDoubleImpl(bodyString));
        }
        if (typeName.equals("float")) {
            return (new XsFloatImpl(bodyString));
        }
        if (typeName.equals("array")) {
            return new JSArrayImpl(bodyString);
        }
        if (typeName.equals("map") || 
            typeName.equals("object") ||
            typeName.equals("jsfunction")) {
            return new JSObjectImpl(bodyString);
        }
        if (typeName.equals("base64Binary")) {
            return (new XsBase64BinaryImpl(bodyString));
        }
        if (typeName.equals("hexBinary")) {
            return (new XsHexBinaryImpl(bodyString));
        }
        if (typeName.equals("untypedAtomic")) {
            return (new XsUntypedAtomicImpl(bodyString));
        }
        if (typeName.equals("anySimpleType")) {
            return (new XsUntypedAtomicImpl(bodyString)); // note: treated as xs:untypedAtomic
        }
        if (typeName.equals("date")) {
            return (new XsDateImpl(bodyString, timezone, locale));
        }
        if (typeName.equals("dateTime")) {
            return (new XsDateTimeImpl(bodyString, timezone, locale));
        }
        if (typeName.equals("time")) {
            return (new XsTimeImpl(bodyString, timezone, locale));
        }
        if (typeName.equals("gDay")) {
            return (new XsGDayImpl(bodyString, timezone, locale));
        }
        if (typeName.equals("gMonth")) {
            return (new XsGMonthImpl(bodyString, timezone, locale));
        }
        if (typeName.equals("gMonthDay")) {
            return (new XsGMonthDayImpl(bodyString, timezone, locale));
        }
        if (typeName.equals("gYear")) {
            return (new XsGYearImpl(bodyString, timezone, locale));
        }
        if (typeName.equals("gYearMonth")) {
            return (new XsGYearMonthImpl(bodyString, timezone, locale));
        }
        if (typeName.equals("duration")) {
            return (new XsDurationImpl(bodyString));
        }
        if (typeName.equals("dayTimeDuration")) {
            return (new XsDayTimeDurationImpl(bodyString));
        }
        if (typeName.equals("yearMonthDuration")) {
            return (new XsYearMonthDurationImpl(bodyString));
        }
        if (typeName.equals("box")) {
            return (new CtsBoxImpl(bodyString));
        }
        if (typeName.equals("circle")) {
            return (new CtsCircleImpl(bodyString));
        }
        if (typeName.equals("point")) {
            return (new CtsPointImpl(bodyString));
        }
        if (typeName.equals("polygon")) {
            return (new CtsPolygonImpl(bodyString));
        }

        throw new IOException("Unrecognized atomic item type: " + typeName);
    }
}
