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
package com.marklogic.xcc.types;

/**
 * An XDM-like binary() value (binary() is a MarkLogic extension).
 */
public interface XdmBinary extends XdmNode {
    /**
     * Buffers the binary() item from the server and converts it to a Java byte array. This method
     * may be invoked repeatedly. On subsequent invocations it will return the same array
     * constructed by the first invocation.<br>
     * <strong>NOTE:</strong> If the binary() item is large, it is possible that an OutOfMemory
     * error could result when invoking this method, which could result in your program crashing. If
     * you need to handle arbitrarily large binary() items, use the {@link #asInputStream()} method.
     * 
     * @return This item as a Java byte array.
     * @throws IllegalStateException
     *             If called after the InputStream has already been consumed.
     * @see #asInputStream()
     * @see #isCached()
     */
    byte[] asBinaryData();
}
