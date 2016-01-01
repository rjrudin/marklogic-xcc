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
package com.marklogic.xcc.spi;

import java.net.InetSocketAddress;

/**
 * This interface should be implemented by {@link com.marklogic.xcc.spi.ConnectionProvider}
 * implementations that make socket connections to a single host and port. The value returned by
 * {@link #getAddress()} will be used to reconstruct the connection {@link java.net.URI} when
 * needed. Custom {@link com.marklogic.xcc.spi.ConnectionProvider} implementations that are not
 * point-to-point oriented should not implement this interface.
 */
public interface SingleHostAddress {
    /**
     * The address to which connections are made.
     * 
     * @return An instance of {@link java.net.InetSocketAddress} that represents host/port to which
     *         connections are made.
     */
    InetSocketAddress getAddress();
}
