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
 * <p>
 * A type-safe enumeration of {@link ResultSequence} channel names.
 * </p>
 * <p>
 * As of the 3.1 release, only the {@link #PRIMARY} channel will contain meaningful information. The
 * {@link #WARNINGS} channel is defiend but will always be empty. Additional channels may become
 * available in future releases.
 * </p>
 */
public class ResultChannelName {
    /** The primary result channel (the actual result). */
    public static final ResultChannelName PRIMARY = new ResultChannelName("primary");

    /**
     * Any warnings related to the result.
     */
    public static final ResultChannelName WARNINGS = new ResultChannelName("warnings");

//	/** Any log messages generated during execution of the query. */
//	public static final ResultChannelName LOG_OUTPUT = new ResultChannelName ("log output");
//
//	/** Any profiling information related to execution of the query. */
//	public static final ResultChannelName PROFILE_DATA = new ResultChannelName ("profile data");

    private String name;

    private ResultChannelName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return (name);
    }
}
