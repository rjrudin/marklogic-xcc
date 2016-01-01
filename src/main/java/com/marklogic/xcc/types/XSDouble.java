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
package com.marklogic.xcc.types;

import java.math.BigDecimal;

/**
 * XDM type: xs:double.
 */
public interface XSDouble extends XdmAtomic {
    /**
     * The item's value as a {@link java.lang.Double}. Some precision may be lost by casting to the
     * Java type.
     * 
     * @see #asBigDecimal()
     * @return The value of this item as a Java Double object.
     */
    java.lang.Double asDouble();

    /**
     * The item's value as a primitive double value. Some precision may be lost by casting to the
     * Java type.
     * 
     * @see #asBigDecimal()
     * @return The value of this item as a primitive Java double.
     */
    double asPrimitiveDouble();

    /**
     * The item's value as a {@link java.math.BigDecimal}. This value may preserve more precision
     * than {@link #asDouble()} or {@link #asPrimitiveDouble()}.
     * 
     * @return The value of this item as a Java BigDecimal object.
     */
    BigDecimal asBigDecimal();
}
