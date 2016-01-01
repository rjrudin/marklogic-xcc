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
package com.marklogic.xcc;

/**
 * <p>
 * An specialization of {@link Request} that contains the URI of an XQuery module (resident in the
 * server) to be evaluated by the MarkLogic Server.
 * </p>
 * <p>
 * XCC can connect to a 3.0 MarkLogic Server, but there was an change in the way module invocations
 * are encoded between 3.0 and 3.1. If you wish use XCC to invoke or spawn a module on a 3.0 server,
 * you must set the system property "xcc.module.invoke.oldstyle" to the value "true". System
 * properties can be set at JVM startup with a command line argument of the form
 * <coe>-Dproperty=value</code> or by calling {@link System#setProperty(String, String)}
 * programmatically.
 * </p>
 */
public interface ModuleInvoke extends Request {
    /**
     * Replace the URI associated with this {@link Request}, which specifies an XQuery module
     * (server-side code) to be invoked on the server.
     * 
     * @param uri
     *            A String that represents the URI of a text document known to the server which is
     *            an XQuery module.
     */
    void setModuleUri(String uri);

    /**
     * Returns the currently set URI for this {@link Request}.
     * 
     * @return The URI, as a String, of a module to invoke upon the next invocation of
     *         {@link Session#submitRequest(Request)}.
     */
    String getModuleUri();
}
