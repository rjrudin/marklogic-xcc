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

import java.util.Arrays;

import javax.net.ssl.SSLContext;

/**
 * This class contains the SSL security options to be used by secure XCC connections. Secure
 * ContentSource objects
 */
public class SecurityOptions {
    private final SSLContext sslContext;
    private String[] enabledProtocols = null;
    private String[] enabledCipherSuites = null;

    /**
     * Construct a new SecurityOptions instance with the specified SSL context.
     * 
     * @param context
     *            The SSL context.
     */

    public SecurityOptions(SSLContext context) {
        sslContext = context;
    }

    public SecurityOptions(SecurityOptions prototype) {
        this.sslContext = prototype.sslContext;
        setEnabledProtocols(prototype.enabledProtocols);
        setEnabledCipherSuites(prototype.enabledCipherSuites);
    }

    /**
     * <p>
     * Return the names of the protocol versions which are to be enabled when new XCCS connections
     * are created. The returned array is a sorted <i>copy</i> of the protocols list; changes to it
     * will not affect this object or anything that uses it.
     * </p>
     * 
     * @return An array of protocols, or null if the SSLContext's defaults are to be used.
     * @see #setEnabledProtocols(String[] enabledProtocols)
     */

    public String[] getEnabledProtocols() {
        return (enabledProtocols != null) ? enabledProtocols.clone() : null;
    }

    /**
     * <p>
     * Return the SSL Context that will be used for new XCCS connections.
     * </p>
     * 
     * @return The SSLContext object.
     */

    public SSLContext getSslContext() {
        return sslContext;
    }

    /**
     * <p>
     * Set the protocol versions enabled when new XCCS connections are created. Following a
     * successful call to this method, only protocols listed in the protocols parameter are enabled
     * for use.
     * </p>
     * 
     * @param enabledProtocols
     *            Names of all the protocols to enable.
     * @see #getEnabledProtocols()
     */

    public void setEnabledProtocols(String[] enabledProtocols) {
        if (enabledProtocols == null) {
            this.enabledProtocols = null;
        } else {
            enabledProtocols = enabledProtocols.clone();
            Arrays.sort(enabledProtocols);
            this.enabledProtocols = enabledProtocols;
        }
    }

    /**
     * <p>
     * Return the names of the SSL cipher suites which are to be enabled when new XCCS connections
     * are created.
     * </p>
     * <p>
     * Even if a suite has been enabled, it might never be used. (For example, the peer does not
     * support it, the requisite certificates (and private keys) for the suite are not available, or
     * an anonymous suite is enabled but authentication is required. The returned array is a sorted
     * <i>copy</i> of the cipher suites list; changes to it will not affect this object or anything
     * that uses it.
     * </p>
     * 
     * @return An array of enabled cipher suite names, or null if the SSL context's defaults are to
     *         be used.
     * @see #setEnabledCipherSuites(String[] enabledCipherSuites)
     */

    public String[] getEnabledCipherSuites() {
        return (enabledCipherSuites != null) ? enabledCipherSuites.clone() : null;
    }

    /**
     * <p>
     * Set the cipher suites enabled when new XCCS connections are created. Following a successful
     * call to this method, only suites listed in the suites parameter are enabled for use.
     * </p>
     * <p>
     * See {@link #getEnabledCipherSuites()} for more information on why a specific cipher suite may
     * never be used on a connection.
     * </p>
     * 
     * @param enabledCipherSuites
     *            Names of all the cipher suites to enable.
     * @see #getEnabledCipherSuites()
     */

    public void setEnabledCipherSuites(String[] enabledCipherSuites) {
        if (enabledCipherSuites == null) {
            this.enabledCipherSuites = null;
        } else {
            enabledCipherSuites = enabledCipherSuites.clone();
            Arrays.sort(enabledCipherSuites);
            this.enabledCipherSuites = enabledCipherSuites;
        }
    }

    /**
     * Returns a computed hash based on the enabled cipher and protocol names, and the hashCode of
     * the sslContext, if set.
     * 
     * @return a content-based hash code for this object.
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(enabledCipherSuites) + Arrays.hashCode(enabledProtocols) + ((sslContext != null) ? sslContext.hashCode() : 0);
    }

    /**
     * <p>
     * Returns true if the passed object is a SecurityOptions instance that has the same enabled
     * ciphers and protocols, and references the same SSLContext instance.
     * </p>
     * 
     * @param o
     *            the reference object with which to compare.
     * @return if this object is the same as the obj argument; false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof SecurityOptions) {
            if (this == o) {
                return true;
            } else {
                SecurityOptions so = (SecurityOptions)o;
                return (
                (sslContext == so.sslContext) &&
                Arrays.equals(enabledCipherSuites, so.enabledCipherSuites) && Arrays.equals(enabledProtocols,
                        so.enabledProtocols));
            }
        } else {
            return false;
        }
    }
}
