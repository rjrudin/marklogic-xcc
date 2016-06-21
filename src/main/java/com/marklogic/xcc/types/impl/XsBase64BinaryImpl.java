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
package com.marklogic.xcc.types.impl;

import com.marklogic.io.Base64;
import com.marklogic.xcc.types.ValueType;
import com.marklogic.xcc.types.XSBase64Binary;

public class XsBase64BinaryImpl extends AbstractStringItem implements XSBase64Binary {
    public XsBase64BinaryImpl(String bodyString) {
        super(ValueType.XS_BASE64_BINARY, bodyString);
        Base64.decode(bodyString); // DAL: here just to test if the data is valid bug: 26072
    }
    
    public XsBase64BinaryImpl(byte[] bodyBytes) {
        super(ValueType.XS_BASE64_BINARY, Base64.encodeBytes(bodyBytes, 0, bodyBytes.length) );
    }

    public byte[] asBinaryData() {
        return Base64.decode(asString());
    }
}
