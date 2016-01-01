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
package com.marklogic.xcc.impl.handlers;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import com.marklogic.xcc.Request;
import com.marklogic.xcc.ValueFactory;
import com.marklogic.xcc.exceptions.JavaScriptException;
import com.marklogic.xcc.exceptions.QueryException;
import com.marklogic.xcc.exceptions.RequestServerException;
import com.marklogic.xcc.exceptions.RetryableJavaScriptException;
import com.marklogic.xcc.exceptions.RetryableXQueryException;
import com.marklogic.xcc.exceptions.XQueryException;
import com.marklogic.xcc.exceptions.QueryStackFrame;
import com.marklogic.xcc.types.XName;
import com.marklogic.xcc.types.XdmVariable;

/**
 * <p>
 * Class to parse error documents returned by the server and turn them into Java Exceptions and
 * Warnings. Uses a simple SAX parser for simplicity and speed.
 * </p>
 */

public class ServerErrorParser {
    private static final boolean VALIDATING = false;
    private static final boolean NAMESPACE = true;

    private static final SAXParserFactory factory;

    static {
        factory = SAXParserFactory.newInstance();
        factory.setValidating(VALIDATING);
        factory.setNamespaceAware(NAMESPACE);
    }

    private ServerErrorParser() {
        // class cannot be instantiated
    }

    // --------------------------------------------------------------

    public static RequestServerException makeException(Request request, String exceptionNode) {
        List<RequestServerException> exceptions = new ArrayList<RequestServerException>();

        try {
            InputSource is = new InputSource(new StringReader(exceptionNode));
            SAXParser parser = factory.newSAXParser();
            SaxHandler handler = new SaxHandler(request, exceptions);
            XMLReader xmlr = parser.getXMLReader();

            xmlr.setContentHandler(handler);
            xmlr.setErrorHandler(handler);
            parser.parse(is, handler);

        } catch (SAXParseException e) {
            return new RequestServerException("SAX Error parsing server exception: " + e, request, e);

        } catch (Exception e) {
            String message = e.getMessage();

            if (message == null) {
                message = e.getClass().getName();
            }

            exceptions.add(new RequestServerException("ErrorParser: " + message, request, e));
        }

        if (exceptions.size() == 0) {
            return new RequestServerException("Failed to parse server exception: " + exceptionNode, request);
        }

        return exceptions.get(0);
    }

    private static class SaxHandler extends DefaultHandler {
        private final Request request;
        private final List<RequestServerException> exceptionList;
        private final StringBuffer sb = new StringBuffer(512);
        private String code = null;
        private String w3cCode = null;
        private String xqueryVersion = null;
        private LinkedList<String> xqueryVersionStack = new LinkedList<String>();
        private String formatString = null;
        private boolean retryable = false;
        private String contextItem = null;
        private String message = null;
        private String name = null;
        private String expr = null;
        private String operation = null;
        private String uri = null;
        private String value = null;
        private int contextPosition = 0;
        private int line = 0;
        private List<String> dataList = new ArrayList<String>();
        private List<QueryStackFrame> stackList = new ArrayList<QueryStackFrame>();
        private List<String> variablesList = new ArrayList<String>();
        private boolean isJavaScript;

        public SaxHandler(Request request, List<RequestServerException> exceptionList) {
            this.request = request;
            this.exceptionList = exceptionList;
            if (request != null && request.getOptions() != null && 
                "javascript".equalsIgnoreCase(
                        request.getOptions().getQueryLanguage())) {
                isJavaScript = true;
            }
        }

        /**
         * org.xml.sax interface callback method
         */
        @Override
        public void startDocument() {
        }

        /**
         * org.xml.sax interface callback method
         */
        @Override
        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) {
            sb.setLength(0);

            if (localName.equals("data")) {
                dataList.clear();
                return;
            }

            if (localName.equals("error")) {
                code = null;
                w3cCode = null;
                message = null;
                formatString = null;
                retryable = false;
                xqueryVersionStack.clear();
                xqueryVersion = "0.9-ml";
                dataList.clear();
                stackList.clear();
                return;
            }

            if (localName.equals("frame")) {
                uri = null;
                line = 0;
                operation = null;
                variablesList.clear();
                contextItem = null;
                contextPosition = 0;
                xqueryVersionStack.addLast(xqueryVersion);
                xqueryVersion = "0.9-ml";
                return;
            }

            if (localName.equals("stack")) {
                stackList.clear();
                return;
            }

            if (localName.equals("variable")) {
                name = null;
                value = null;
                return;
            }

            if (localName.equals("variables")) {
                variablesList.clear();
                return;
            }
        }

        /**
         * org.xml.sax interface callback method
         */
        @Override
        public void characters(char[] ch, int start, int length) {
            sb.append(ch, start, length);
        }

        /**
         * org.xml.sax interface callback method
         */
        @Override
        public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
            if (localName.equals("code")) {
                code = sb.toString();
            }

            if (localName.equals("name")) {
                w3cCode = sb.toString();
                if ((w3cCode != null) && w3cCode.length() == 0) {
                    w3cCode = null;
                }
            }

            if (localName.equals("xquery-version")) {
                xqueryVersion = sb.toString();

                if ((xqueryVersion != null) && xqueryVersion.length() == 0) {
                    xqueryVersion = null;
                }
            }

            if (localName.equals("context-item")) {
                contextItem = sb.toString();
            }

            if (localName.equals("context-position")) {
                contextPosition = Integer.parseInt(sb.toString());
            }

            if (localName.equals("datum")) {
                dataList.add(sb.toString());
            }

            if (localName.equals("error")) {
                String[] data = null;

                data = dataList.toArray(new String[dataList.size()]);

                QueryStackFrame[] stack = null;

                stack = stackList.toArray(new QueryStackFrame[stackList.size()]);

                QueryException eee;

                if (retryable) {
                    if (isJavaScript) {
                        eee = new RetryableJavaScriptException(request, code, 
                                w3cCode, message, formatString,
                                expr, retryable, data, stack);
                    } else {
                        eee = new RetryableXQueryException(request, code, 
                                w3cCode, xqueryVersion, message, formatString,
                                expr, retryable, data, stack);
                    }
                } else {
                    if (isJavaScript) {
                        eee = new JavaScriptException(request, code, 
                                w3cCode, message, formatString,
                                expr, retryable, data, stack);
                    } else {
                        eee = new XQueryException(request, code, w3cCode, 
                                xqueryVersion, message, formatString, expr,
                                retryable, data, stack);
                    }
                }

                exceptionList.add(eee);
                code = null;
                w3cCode = null;
                message = null;
                formatString = null;
                retryable = false;
                xqueryVersionStack.clear();
                dataList.clear();
                stackList.clear();

                // This is the end of the outer-most containing element
                return;
            }

            if (localName.equals("expr")) {
                expr = sb.toString();
            }

            if (localName.equals("format-string")) {
                formatString = sb.toString();
            }

            if (localName.equals("frame")) {
                XdmVariable[] variables = null;

                variables = variablesList.toArray(new XdmVariable[variablesList.size()]);

                stackList.add(new QueryStackFrame(uri, line, operation, variables, contextItem, contextPosition,
                        xqueryVersion));

                uri = null;
                line = 0;
                operation = null;
                variablesList.clear();
                xqueryVersion = xqueryVersionStack.removeLast();
                contextItem = null;
                contextPosition = 0;
            }

            if (localName.equals("line")) {
                line = Integer.parseInt(sb.toString());
            }

            if (localName.equals("message")) {
                message = sb.toString();
            }

            if (localName.equals("name")) {
                name = sb.toString();
            }

            if (localName.equals("operation")) {
                operation = sb.toString();
            }

            if (localName.equals("retryable")) {
                retryable = sb.toString().equals("true");
            }

            if (localName.equals("title")) {
                exceptionList.add(new RequestServerException(sb.toString(), request));
            }

            if (localName.equals("uri")) {
                uri = sb.toString();
            }

            if (localName.equals("value")) {
                value = sb.toString();
            }

            if (localName.equals("variable")) {
                if (variablesList != null) {
                    ValueFactory.newVariable(new XName(name), ValueFactory.newXSString(value));
                }

                name = null;
                value = null;
            }

            sb.setLength(0);
        }

        /**
         * org.xml.sax interface callback method
         */
        @Override
        public void endDocument() {
        }

        /**
         * org.xml.sax interface callback method
         */
        @Override
        public void warning(SAXParseException e) {
            String message = e.getMessage();

            if (message == null) {
                message = e.getClass().getName();
            }

            exceptionList.add(new RequestServerException("ErrorParser: SAX warning: " + message, request, e));
        }

        /**
         * org.xml.sax interface callback method
         */
        @Override
        public void error(SAXParseException e) {
            String message = e.getMessage();

            if (message == null) {
                message = e.getClass().getName();
            }

            exceptionList.add(new RequestServerException("ErrorParser: SAX error: " + message, request, e));
        }

//		public void reduceStackTrace (RequestServerException xe)
//		{
//			StackTraceElement [] oldstack = xe.getStackTrace();
//
//			if (oldstack == null || oldstack.length < 1) {
//				return;
//			}
//
//			List newvec = new ArrayList (oldstack.length);
//
//			for (int i = oldstack.length; i > 0; i--) {
//				StackTraceElement ste = oldstack [i - 1];
//				String cls = ste.getClassName();
//
//				if (cls != null && (cls.startsWith ("com.marklogic.xdmp")
//					|| cls.startsWith ("com.marklogic.xdbc")))
//				{
//					break;
//				} else {
//					newvec.add (0, ste);
//				}
//			}
//
//			StackTraceElement[] newstack = (StackTraceElement[]) newvec.toArray (new StackTraceElement[newvec.size ()]);
//			xe.setStackTrace (newstack);
//		}

        /**
         * org.xml.sax interface callback method
         */
        @Override
        public void fatalError(SAXParseException e) {
            // Don't signal parsing exceptions during error handling
            // since the stream frequently contains extra stuff that
            // causes the error node to not be well-formed.

            //      e.printStackTrace();
            //      String message = e.getMessage();
            //      if (message == null) message = e.getClass().getName();
            //      parser.addException(
            //	new XDBCException(
            //	  "ErrorParser: SAX fatal error: "+message,
            //	  e));
        }

        /**
         * org.xml.sax interface callback method
         */
//		private String getLocationString (SAXParseException ex)
//		{
//			StringBuffer str = new StringBuffer ();
//			String systemId = ex.getSystemId ();
//			if (systemId != null) {
//				int index = systemId.lastIndexOf ('/');
//				if (index != -1) {
//					systemId = systemId.substring (index + 1);
//				}
//				str.append (systemId);
//			}
//			str.append (':');
//			str.append (ex.getLineNumber ());
//			str.append (':');
//			str.append (ex.getColumnNumber ());
//			return str.toString ();
//		}
    }

    // ----------------------------------------------------------------

//	public static void main (String[] args)
//	{
//		String frame =
//			"<error xsi:schemaLocation=\"http://marklogic.com/xdmp/error error.xsd\""
//				+ " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
//				+ " xmlns=\"http://marklogic.com/xdmp/error\">"
//				+ "  <code>cannot fn:darin(document(\"books.xml\")/bk:books) -- arg1 ($t) is document(\"books.xml\")/bk:books, which is not of type empty</code>"
//				+ "  <message>cannot fn:darin(document(\"books.xml\")/bk:books) -- arg1 ($t) is document(\"books.xml\")/bk:books, which is not of type empty</message>"
//				+ "  <format-string>%m</format-string>"
//				+ "  <retryable>false</retryable>"
//				+ "  <data/>"
//				+ "  <expr/>"
//				+ "  <stack>"
//				+ "    <frame>"
//				+ "      <line>9</line>"
//				+ "      <variables>"
//				+ "        <variable>"
//				+ "          <name xmlns=\"\">book</name>"
//				+ "          <value xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">document(\"books.xml\")/bk:books</value>"
//				+ "        </variable>"
//				+ "      </variables>"
//				+ "    </frame>"
//				+ "  </stack>"
//				+ "</error>";
//
//		ServerErrorParser xep = new ServerErrorParser ();
//		RequestServerException xex = xep.makeException (frame);
//
//		//noinspection UseOfSystemOutOrSystemErr
//		System.out.println ("XDBCException is " + xex);
//	}
}
