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
package com.marklogic.xcc;

/**
 * An extension of {@link com.marklogic.xcc.Request} that contains the URI of an XQuery module
 * (resident server-side) to be spawned asynchronously by the MarkLogic Server.
 */
public interface ModuleSpawn extends Request {
    /**
     * Replace the URI associated with this {@link com.marklogic.xcc.Request}, which specifies an
     * XQuery module (server-side code) to be spawned (run asynchronously) on the server.
     * 
     * @param uri
     *            A String that represents the URI of a text document known to the server which is
     *            an XQuery module.
     */
    void setModuleUri(String uri);

    /**
     * Returns the currently set URI for this {@link com.marklogic.xcc.Request}.
     * 
     * @return The URI, as a String, of a module to invoke upon the next invocation of
     *         {@link com.marklogic.xcc.Session#submitRequest(com.marklogic.xcc.Request)}.
     */
    String getModuleUri();

//	String getModuleRoot();
}
