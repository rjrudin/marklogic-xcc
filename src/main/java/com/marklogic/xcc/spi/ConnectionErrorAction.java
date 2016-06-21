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
package com.marklogic.xcc.spi;

/**
 * This is a type-safe enumeration that defines the possible return values of
 * {@link ConnectionProvider#returnErrorConnection(ServerConnection, Throwable, java.util.logging.Logger)}
 * .
 */
public class ConnectionErrorAction {
    private String name;

    private ConnectionErrorAction(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return (name);
    }

    public static final ConnectionErrorAction FAIL = new ConnectionErrorAction("fail");
    public static final ConnectionErrorAction RETRY = new ConnectionErrorAction("retry");
}
