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
package com.marklogic.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.List;

import com.marklogic.xcc.exceptions.UnexpectedResponseException;

public class HttpHeaders {
    private static final String PREFIX = "X-X-";
    private static final String HTTP_RESPONSE_KEY = PREFIX + "HTTP-RESPONSE-LINE";
    private static final String RESPONSE_CODE_KEY = PREFIX + "HTTP-RESPONSE-CODE";
    private static final String RESPONSE_MSG_KEY = PREFIX + "HTTP-RESPONSE-MESSAGE";

    private static final String HTTP_REQUEST_KEY = PREFIX + "HTTP-REQUEST-LINE";
    private static final String REQUEST_METHOD_KEY = PREFIX + "HTTP-REQUEST-METHOD";
    private static final String REQUEST_PATH_KEY = PREFIX + "HTTP-REQUEST-PATH";
    private static final String REQUEST_VERSION_KEY = PREFIX + "HTTP-REQUEST-VERSION";

    private Map<String, List<String>> headers = new LinkedHashMap<String, List<String>>();

    // ---------------------------------------------------------------

    public void clear() {
        headers.clear();
    }

    public int size() {
        return (headers.size());
    }

    public void setHeaderNormalized(String name, String value) {
        setHeader(name.toLowerCase(), value);
    }

    public void setHeader(String name, String value) {
        List<String> l = headers.get(name);
        if(l == null) {
            l = new LinkedList<String>();
            headers.put(name, l);
        }
        l.add(value);
    }

    public List<String> getAllHeadersNormalized(String name) {
        return getAllHeaders(name.toLowerCase());
    }

    public List<String> getAllHeaders(String name) {
        List<String> vals = headers.get(name);
        return vals == null ? null : Collections.unmodifiableList(vals);
    }

    public String getHeaderNormalized(String name) {
        return getHeader(name.toLowerCase());
    }

    public String getHeader(String name) {
        List<String> vals = headers.get(name);
        return vals == null ? null : vals.get(vals.size() - 1);
    }

    // ---------------------------------------------------------------

    public Iterator<String> iterator() {
        Set<String> keys = new LinkedHashSet<String>(headers.size());

        for (Iterator<String> it = headers.keySet().iterator(); it.hasNext();) {
            String key = it.next();

            if (key.startsWith(PREFIX)) {
                continue;
            }

            keys.add(key);
        }

        return (keys.iterator());
    }

    public Iterator<String> iteratorAll() {
        return (Collections.unmodifiableMap(headers).keySet().iterator());
    }

    // ---------------------------------------------------------------

    public void setResponseValues(String line) throws IOException {
        setHeader(HTTP_RESPONSE_KEY, line);

        String[] parts = line.split("\\s+", 3);

        if ((parts.length < 2) || (!parts[0].startsWith("HTTP/1.") && !parts[0].startsWith("XDBC/"))) {
            throw new IOException("Malformed Response: " + line);
        }

        String codeStr = parts[1];

        try {
            Integer.parseInt(codeStr);
            setHeader(RESPONSE_CODE_KEY, codeStr);
        } catch (NumberFormatException e) {
            throw new IOException("Malformed Response code: " + codeStr);
        }

        String msg = (parts.length > 2) ? parts[2] : "";

        setHeader(RESPONSE_MSG_KEY, msg);
    }

    public String getResponseLine() {
        return (getHeader(HTTP_RESPONSE_KEY));
    }

    public int getResponseCode() {
        String hdr = getHeader(RESPONSE_CODE_KEY);

        if (hdr == null) {
            return (-1);
        }

        return Integer.parseInt(hdr);
    }

    public String getResponseMessage() {
        return (getHeader(RESPONSE_MSG_KEY));
    }

    public void setRequestValues(String method, String path, String version) {
        setHeader(REQUEST_METHOD_KEY, method);
        setHeader(REQUEST_PATH_KEY, path);
        setHeader(REQUEST_VERSION_KEY, version);

        setHeader(HTTP_REQUEST_KEY, method + " " + path + " " + version);
    }

    public String getRequestLine() {
        return (getHeader(HTTP_REQUEST_KEY));
    }

    public String getRequestMethod() {
        return (getHeader(REQUEST_METHOD_KEY));
    }

    public String getRequestPath() {
        return (getHeader(REQUEST_PATH_KEY));
    }

    public String getRequestVersion() {
        return (getHeader(REQUEST_VERSION_KEY));
    }

    public int getContentLength() {
        String lengthStr = getHeaderNormalized("content-length");

        if (lengthStr == null) {
            return (-1);
        }

        try {
            return (Integer.parseInt(lengthStr));
        } catch (NumberFormatException e) {
            return (-1);
        }
    }

    public String getContentType() {
        String value = getHeaderNormalized("content-type");

        if (value == null) {
            return (null);
        }

        String[] parts = value.split("\\s*;\\s*");

        return (parts[0]);
    }

    public String getContentTypeField(String fieldName) {
        return (getHeaderSubValue("content-type", fieldName, ";"));
    }

    // ---------------------------------------------------------------

    public StringBuffer toStringBuffer(StringBuffer userSb) {
        StringBuffer sb = (userSb == null) ? new StringBuffer(1024) : userSb;
        String lineSep = "\r\n";

        sb.append(getRequestLine());
        sb.append(lineSep);

        for (Iterator<String> it = iterator(); it.hasNext();) {
            String key = it.next();
            for(String val: getAllHeaders(key)) {
                sb.append(key).append(": ").append(val);
                sb.append(lineSep);
            }
        }

        sb.append(lineSep);

        return (sb);
    }

    @Override
    public String toString() {
        return (toStringBuffer(null).toString());
    }

    public int writeHeaders(OutputStream os) throws IOException {
        byte[] bytes = toString().getBytes("UTF-8");

        os.write(bytes);

        return (bytes.length);

//		ByteArrayOutputStream bos = new ByteArrayOutputStream (256);
//		byte [] lineSepBytes = "\r\n".getBytes ("UTF-8");
//
//		bos.write (getRequestLine ().getBytes());
//		bos.write (lineSepBytes);
//
//		for (Iterator it = iterator(); it.hasNext ();) {
//			String key = (String) it.next ();
//
//			String header = key + ": " + getHeader (key);
//
//			bos.write (header.getBytes ("UTF-8"));
//			bos.write (lineSepBytes);
//		}
//
//		bos.write (lineSepBytes);
//		bos.flush ();
//
//		byte[] headerBytes = bos.toByteArray();
//
//		os.write (headerBytes);
//
//		return (headerBytes.length);
    }

    // ---------------------------------------------------------------

    public void parseResponseHeaders(InputStream is) throws IOException {
        clear();

        String line;

        try {
            line = nextHeaderLine(is);
        } catch (Exception e) {
            IOException newex = new IOException(
                    "Error parsing HTTP headers: " + e.getMessage(), e);

            newex.setStackTrace(e.getStackTrace());

            throw newex;
        }

        setResponseValues(line);

        parsePlainHeaders(is);
    }

    public void parsePlainHeaders(InputStream is) throws IOException {
        String line;
        try {
            while ((line = nextHeaderLine(is)) != null) {
    //            String[] hdrParts = line.split("\\s*:\\s*", 2);
    //
    //            if (hdrParts.length != 2) {
    //                throw new IOException("Malformed header line: " + line);
    //            }
    //
    //            setHeaderNormalized(hdrParts[0], hdrParts[1]);
                int i = line.indexOf(':');
                setHeaderNormalized(line.substring(0,i),line.substring(i+2));
            }
        } catch (Exception e) {
            IOException newex = new IOException(
                    "Error parsing HTTP headers: " + e.getMessage(), e);
            throw newex;
        }
    }

    private String nextHeaderLine(InputStream is) 
    throws UnexpectedResponseException, IOException {
        StringBuilder sb = new StringBuilder(64);

        while (true) {
            int b = is.read();

            if (b == -1) {
                throw new UnexpectedResponseException(
                        "Premature EOF, partial header line read: '" + sb.toString() + "'", 
                        sb.toString());
            }

            if (b == '\r') {
                continue;
            }

            if (b == '\n') {
                break;
            }

            sb.append((char)b);
        }

        return (sb.length() > 0) ? sb.toString() : null;
    }

    // ---------------------------------------------------------------

    public String getHeaderSubValue(String headerName, String subName, String delim) {
        List<String> values = getAllHeaders(headerName);
        if(values != null) {
            for(String header: values) {
                String val = getHeaderSubValueFromValue(header, subName, delim);
                if(val != null) return val;
            }
        }
        return null;
    }
    
    public static String getHeaderSubValueFromValue(String headerValue, String subName, String delim) {
        if (headerValue == null) {
            return (null);
        }

        String[] parts = headerValue.split("\\s*" + delim + "\\s*");

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];

            if (part.startsWith(subName)) {
                String[] subParts = part.split("\\s*=\\s*");

                if (subParts.length == 2) {
                    return (subParts[1]);
                }
            }
        }

        return (null);
    }

    public Integer getHeaderSubValueInt(String headerName, String subName, String delim) {
        String strValue = getHeaderSubValue(headerName, subName, delim);

        if (strValue == null) {
            return (null);
        }

        try {
            return (Integer.decode(strValue));
        } catch (NumberFormatException e) {
            return (null);
        }
    }

    // ---------------------------------------------------------------

    static String extractCsvSubValue(String csv, String subName) {
        String[] parts = csv.split("\\s*,\\s*");

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];

            if (part.startsWith(subName)) {
                String[] subParts = part.split("\\s*=\\s*");

                if (subParts.length == 2) {
                    return (subParts[1]);
                }
            }
        }

        return (null);
    }

    static Integer extractCsvIntSubValue(String csv, String subName) {
        String subString = extractCsvSubValue(csv, subName);

        if (subString == null) {
            return (null);
        }

        try {
            return Integer.decode(subString);
        } catch (NumberFormatException e) {
            return (null);
        }
    }

    // ---------------------------------------------------------------
}
