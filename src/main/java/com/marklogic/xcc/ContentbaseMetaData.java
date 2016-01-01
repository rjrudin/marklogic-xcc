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

import java.math.BigInteger;
import java.util.Map;

import com.marklogic.xcc.exceptions.RequestException;

/**
 * <p>
 * Meta-data about a contentbase as represented by a {@link Session} instance. The methods of this
 * class that throw {@link RequestException} may make one or more calls the server to obtain
 * needed information. Those which don't throw exceptions return local information.
 * </p>
 * <p>
 * All server-query methods are lazy and do temporary caching. That is, no server calls are made
 * until you invoke a method that needs information from the server. And once some information is
 * fetched from the server, local copies will be returned on subsequent calls, until the data ages
 * out. The time-to-live for cached information is about 60 seconds.
 * </p>
 */
public interface ContentbaseMetaData {
    /**
     * The driver (XCC) release version, as a String.
     * 
     * @return The String form (M.m-p) of the release version.
     */
    String getDriverVersionString();

    /**
     * The driver (XCC) major release version.
     * 
     * @return The major release number, as an integer.
     */
    int getDriverMajorVersion();

    /**
     * The driver (XCC) minor release version.
     * 
     * @return The minor release number, as an integer.
     */
    int getDriverMinorVersion();

    /**
     * The driver (XCC) patch release version.
     * 
     * @return The minor release number, as an integer.
     */
    int getDriverPatchVersion();

    /**
     * <p>
     * Returns the user identity associated with the {@link Session} from which this object was
     * obtained.
     * </p>
     * <p>
     * This property may also be obtained directly from the {@link Session} object via the
     * {@link com.marklogic.xcc.UserCredentials} object returned by the
     * {@link com.marklogic.xcc.Session#getUserCredentials()} method. It is included here for
     * completeness.
     * </p>
     * 
     * @return The user identity as a String.
     */
    String getUser();

    /**
     * Return the name of the contentbase associated with the {@link Session}. Unlike the similar
     * method {@link com.marklogic.xcc.Session#getContentBaseName()}, this method makes a call to
     * the server to obtain the name. {@link Session} instances may be created without an explicit
     * contentbase name (use a default) or with a numeric ID. This method determines the actual
     * alphanumeric name of the contentbase.
     * 
     * @return The contentbase name as a String.
     * @throws RequestException
     *             If there is a problem communicating with the server.
     */
    String getContentBaseName() throws RequestException;

    /**
     * Return the numeric database ID of the contentbase. Similar to {@link #getContentBaseName()},
     * this method makes a call to the server to obtain the ID, regardless of how the
     * {@link Session} was created.
     * 
     * @return The server's numeric (unsigned long) database ID.
     * @throws RequestException
     *             If there is a problem communicating with the server.
     */
    BigInteger getContentBaseId() throws RequestException;

    /**
     * Return the IDs of forests attached to the contentbase associated with the {@link Session}.
     * 
     * @return An array of {@link BigInteger} values.
     * @throws RequestException
     *             If there is a problem communicating with the server.
     */
    BigInteger[] getForestIds() throws RequestException;

    /**
     * Return the names of the forests attached to the contnetbase associated with the
     * {@link Session}.
     * 
     * @return An array of forest names as {@link String}s.
     * @throws RequestException
     *             If there is a problem communicating with the server.
     */
    String[] getForestNames() throws RequestException;

    /**
     * Returns a {@link Map} of forest names to forest IDs. The keys of the {@link Map} are the
     * forest names and the values are {@link BigInteger} values that represent the forest IDs.
     * 
     * @return A {@link Map}, keyed by forest name, of the forest IDs.
     * @throws RequestException
     *             If there is a problem communicating with the server.
     */
    Map<String, BigInteger> getForestMap() throws RequestException;

    /**
     * Return a String version of the MarkLogic Server.
     * 
     * @return The full server version as a String.
     * @throws RequestException
     *             If there is a problem communicating with the server.
     */
    String getServerVersionString() throws RequestException;

    /**
     * Return the server major release version number.
     * 
     * @return The server major release number as an integer.
     * @throws RequestException
     *             If there is a problem communicating with the server.
     */
    int getServerMajorVersion() throws RequestException;

    /**
     * Return ther server minor release version number.
     * 
     * @return The server minor release number as an integer.
     * @throws RequestException
     *             If there is a problem communicating with the server.
     */
    int getServerMinorVersion() throws RequestException;

    /**
     * Return ther server patch release version number.
     * 
     * @return The server patch release number as an integer.
     * @throws RequestException
     *             If there is a problem communicating with the server.
     */
    int getServerPatchVersion() throws RequestException;

    /**
     * The {@link Session} object that created this object.
     * 
     * @return A {@link Session} instance.
     */
    Session getSession();
}
