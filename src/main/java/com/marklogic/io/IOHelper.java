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
package com.marklogic.io;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class IOHelper {
    private IOHelper() {
        // cannot be instantiated
    }

    public static BufferedReader newReader(String value) {
        return (new BufferedReader(new StringReader(value)));
    }

    public static InputStream newStream(String value) {
        return (new ByteArrayInputStream(getUtf8Bytes(value)));
    }

    public static String literalStringFromReader(Reader reader) throws IOException {
        int SBUF_INIT_SIZE = 32;
        StringBuffer sb = new StringBuffer(SBUF_INIT_SIZE);
        char[] buf = new char[10240];
        int rc;

        while ((rc = reader.read(buf)) > 0) {
            sb.append(buf, 0, rc);
        }

        buf = null; // encourage immediate memory re-use

        if (sb.length() <= SBUF_INIT_SIZE) {
            return sb.toString();
        }

        // Most strings are relatively small.
        // Doing this insures the returned String is exactly the size
        // of the number of chars read.
        return sb.substring(0);
    }

    public static String literalStringFromStream(InputStream is) throws IOException {
        return (literalStringFromReader(new BufferedReader(newUtf8StreamReader(is))));
    }

    public static String stringFromReader(BufferedReader br) throws IOException {
        StringBuffer sb = new StringBuffer();
        String line = null;

        while ((line = br.readLine()) != null) {
            if (sb.length() != 0) {
                sb.append("\n");
            }

            sb.append(line);
        }

        return sb.substring(0);
    }

    public static String stringFromStream(InputStream is) throws IOException {
        return (stringFromReader(new BufferedReader(newUtf8StreamReader(is))));
    }

    public static InputStream newUtf8Stream(String s) {
        try {
            return (new ByteArrayInputStream(s.getBytes("UTF-8")));
        } catch (UnsupportedEncodingException e) {
            // This is extrememly unlikely to happen, UTF-8 is required on all compliant JVMs
            return (new ByteArrayInputStream(s.getBytes()));
        }
    }

    public static Reader newUtf8StreamReader(InputStream is) {
        try {
            return (new InputStreamReader(is, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            // This is extrememly unlikely to happen, UTF-8 is required on all compliant JVMs
            return (new InputStreamReader(is));
        }
    }

    public static BufferedReader newBufferedUtf8Reader(InputStream stream) {
        return new BufferedReader(newUtf8StreamReader(stream));
    }

    public static byte[] byteArrayFromStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[10240];
        int rc;

        while ((rc = inputStream.read(buffer)) > 0) {
            bos.write(buffer, 0, rc);
        }

        return (bos.toByteArray());
    }

    // -------------------------------------------------------

    public static String genericExceptionMessage(Exception e) {
        String message = e.getMessage();

        if (message == null) {
            message = e.getClass().getName();
        }

        return message;
    }

    // ---------------------------------------------------------

    @SuppressWarnings("deprecation")
    public static String urlDecodeString(String value) {
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // should never happen, UFT-8 must be present
            return URLDecoder.decode(value);
        }
    }

    @SuppressWarnings("deprecation")
    public static void urlEncodeToStringBuffer(StringBuffer sb, String value) {
        try {
            urlEncodeStringToStringBuffer(value, sb, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // should never happen, UFT-8 must be present
            sb.append(URLEncoder.encode(value));
        }
    }

    /**
     * Implement a "soft" encode, which hex encodes non-printable chars, '+', '&' and '=' but not
     * other special chars.
     * 
     * @param s
     *            string to be "soft" encoded.
     * @param encoding
     * @return the string after the "soft" encoding has been applied.
     */
    public static String urlEncodeString(String s, String encoding) throws UnsupportedEncodingException {
        StringBuffer sb = new StringBuffer();

        urlEncodeStringToStringBuffer(s, sb, encoding);

        return sb.substring(0);
    }

    /**
     * This is a "quick" URL encoder. If the string contains only ascii printable chars, then a
     * lightweight routine is used to encode the string. Otherwise, the standard library URL encoder
     * is used.
     * 
     * @param s
     *            string to be "soft" encoded.
     * @param sb
     *            A StringBuffer to which the encoded characters will be appended.
     * @param encoding
     */
    public static void urlEncodeStringToStringBuffer(String s, StringBuffer sb, String encoding)
            throws UnsupportedEncodingException {
        if (containsNonAscii(s)) {
            // non-ascii chars seen, use the charset-aware library encoder
            sb.append(URLEncoder.encode(s, encoding));

            return;
        }

        int len = s.length();

        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);

            if (c == ' ') {
                // space is a special case
                sb.append("+");

                continue;
            }

            if (needsEncoding(c)) {
                hexEncodeCharToStringBuffer(sb, c);

                continue;
            }

            sb.append(c);
        }
    }

    // -----------------------------------------------------

    private static boolean needsEncoding(char c) {
        if (Character.isLetterOrDigit(c)) {
            return (false);
        }

        if ((c == '*') || (c == '_') || (c == '.') || (c == '-')) {
            return (false);
        }

        return (true);
    }

    private static boolean containsNonAscii(String s) {
        int len = s.length();

        for (int i = 0; i < len; i++) {
            if (s.charAt(i) > 0x7f) {
                return (true);
            }
        }

        return (false);
    }

    private static void hexEncodeCharToStringBuffer(StringBuffer sb, char c) {
        sb.append("%");

        String hex = Integer.toHexString(c).toUpperCase();

        if (hex.length() == 1) {
            sb.append("0");
        }

        sb.append(hex);
    }

    private static byte[] getUtf8Bytes(String is) {
        try {
            return (is.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            // This is extrememly unlikely to happen, UTF-8 is required on all compliant JVMs
            return (is.getBytes());
        }
    }

    public static String bytesToHex(byte bytes[]) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString((bytes[i]) & 0xff);
            if (hex.length() == 1) {
                buf.append('0');
            }
            buf.append(hex);
        }
        return buf.toString();
    }
}
