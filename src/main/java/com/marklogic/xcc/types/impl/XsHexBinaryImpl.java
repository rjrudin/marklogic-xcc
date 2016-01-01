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
package com.marklogic.xcc.types.impl;

import com.marklogic.xcc.types.ValueType;
import com.marklogic.xcc.types.XSHexBinary;

public class XsHexBinaryImpl extends AbstractStringItem implements XSHexBinary {
    public XsHexBinaryImpl(String bodyString) {
        super(ValueType.XS_HEX_BINARY, bodyString);

        validateHex(bodyString);
    }

    public XsHexBinaryImpl(byte[] body) {
        super(ValueType.XS_HEX_BINARY, convertBinaryToHex(body));
    }

    public byte[] asBinaryData() {
        return (convertHexToBinary(asString()));
    }

    // ---------------------------------------------------------

    private void validateHex(String bodyString) {
        int len = bodyString.length();

        for (int i = 0; i < len; i++) {
            char c = Character.toLowerCase(bodyString.charAt(i));

            if (Character.isDigit(c))
                continue;

            if ((c >= 'a') && (c <= 'f')) {
                continue;
            }

            throw new IllegalArgumentException("Illegal character in hex string: '" + c + "', index=" + i);
        }
    }

    private byte[] convertHexToBinary(String hex) {
        int strLen = hex.length();
        byte[] binary = new byte[(strLen + 1) / 2];
        int binIdx = 0;

        for (int currIdx = 0; currIdx < strLen; currIdx += 2) {
            char hi = hex.charAt(currIdx);
            char lo = (currIdx < (strLen - 1)) ? hex.charAt(currIdx + 1) : '0';
            int val = (Character.digit(hi, 16) << 4) | Character.digit(lo, 16);

            binary[binIdx++] = (byte)(val & 0xff);
        }

        return (binary);
    }

    private static char[] hexDigits = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};

    public static String convertBinaryToHex(byte[] binary) {
        char[] result = new char[binary.length * 2];
        int r = 0;
        for(byte b : binary) {
            result[r++] = hexDigits[(b&0xF0)>>4];
            result[r++] = hexDigits[b&0xF];
        }
        return new String(result);
    }
}
