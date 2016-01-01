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
import java.util.Locale;
import java.util.TimeZone;

import com.marklogic.http.MultipartSplitter;

/**
 * <p>
 * This class represents options to apply to execution of a query. RequestOptions may be set on both
 * {@link Request} and on {@link Session} objects. Options set on {@link Request} take priority. The
 * effective options applied to a request are a blend of of the two objects with defaults applied to
 * values not explicitly set. Use the method {@link com.marklogic.xcc.Request#getEffectiveOptions()}
 * to determine the actual values that will be applied to a given request.
 * </p>
 */
public class RequestOptions {
    /** Default max retry attempts on a request or content insertion (value: 4) */
    public static final int DEFAULT_MAX_AUTO_RETRY = 4;

    /** Default delay (in milliseconds) between automatic query retries (value: 100) */
    public static final int DEFAULT_AUTO_RETRY_DELAY_MILLIS = 100;

    /**
     * The system property name (<code>xcc.request.retries.max</code>) which, if set, specifies the
     * default maximum number of automatic retries. If not set, the programatic default is used (
     * {@link #DEFAULT_MAX_AUTO_RETRY}) as the default.
     */
    public static final String MAX_RETRY_SYSTEM_PROPERTY = "xcc.request.retries.max";

    /**
     * The system property name (<code>xcc.request.retries.delay</code>) which, if set, specifies
     * the default delay (in milliseconds) between automatic request retries. If not set, the
     * programatic default is used ({@link #DEFAULT_AUTO_RETRY_DELAY_MILLIS}) as the default.
     */
    public static final String AUTO_RETRY_DELAY_SYSTEM_PROPERTY = "xcc.request.retries.delay";

    private int maxAutoRetry = -1;
    private int autoRetryDelayMillis = -1;
    private int timeoutMillis = -1;
    private int requestTimeLimit = -1;
    private boolean cacheResult = true;
    private boolean defaultCacheResult = true;
    private String requestName;
    private Locale locale = null;
    private TimeZone timeZone = null;
    private BigInteger effectivePointInTime = null;
    private int resultBufferSize = 0;
    private String defaultXQueryVersion = null;
    private String queryLanguage = null;

    // stuff that's been left out of 3.1, but will be added later
//	private boolean logResultWarnings = true;
//	private Set includedChannels = new HashSet();
//	private Set excludedChannels = new HashSet();
//	private long resultMaxSize = 0;

    // -------------------------------------------------------

    /**
     * <p>
     * Indicates whether the {@link ResultSequence} should be cached when read from the server. The
     * default is true.
     * </p>
     * 
     * @return true if the {@link ResultSequence} should be cached, false if not.
     * @see com.marklogic.xcc.ResultSequence#isCached()
     */
    public boolean getCacheResult() {
        return cacheResult;
    }

    /**
     * Indicates whether the {@link ResultSequence} should be cached. The default is true.
     * 
     * @param cacheResult
     *            Set to true to cause the {@link ResultSequence} to be cached, false if it should
     *            be streamable.
     */
    public void setCacheResult(boolean cacheResult) {
        this.cacheResult = cacheResult;
        defaultCacheResult = false;
    }

    // -------------------------------------------------------

    /**
     * The maximum number of times a retryable request will be automatically retried before throwing
     * an exception. A return value of -1 indicates that a default value will be used. The default
     * is determined by checking for a system property setting ({@link #MAX_RETRY_SYSTEM_PROPERTY})
     * and a programmatic default ({@link #DEFAULT_MAX_AUTO_RETRY}).
     * 
     * @return The currently set max retry value, or -1. The value -1 indicates that a default value
     *         should be used.
     */
    public int getMaxAutoRetry() {
        return maxAutoRetry;
    }

    /**
     * <p>
     * The maximum number of times a retryable request will be automatically retried before throwing
     * an exception. Setting a value of -1 indicates that the default value should be used. A value
     * of zero indicates that no retries should be attempted.
     * </p>
     * <p>
     * Note that this is the number of retries, not the total number of tries. Setting this value to
     * 4, for example, means that the request will be attempted 5 times before giving up.
     * </p>
     * 
     * @param maxAutoRetry
     *            The new max retry value to set. Set to zero for no retries. Set to -1 to apply the
     *            default.
     */
    public void setMaxAutoRetry(int maxAutoRetry) {
        this.maxAutoRetry = maxAutoRetry;
    }

    /**
     * The number of milliseconds to delay between each automatic query retry attempt.
     * 
     * @return The number of milliseconds to delay. Zero means no delay, -1 means use the default (
     *         {@link #DEFAULT_AUTO_RETRY_DELAY_MILLIS}).
     */
    public int getAutoRetryDelayMillis() {
        return autoRetryDelayMillis;
    }

    /**
     * Set the time to delay (in milliseconds) between automatic query retries.
     * 
     * @param autoRetryDelayMillis
     *            Milliseconds to delay (can be zero), -1 means use the default.
     */
    public void setAutoRetryDelayMillis(int autoRetryDelayMillis) {
        this.autoRetryDelayMillis = autoRetryDelayMillis;
    }

    // -------------------------------------------------------

    /**
     * Indcates whether any warnings sent on the {@link ResultChannelName#WARNINGS} channel should
     * be automatically sent to the in-scope Logger object. Either way, any warnings sent will also
     * be available by calling {@link ResultSequence#getChannel(ResultChannelName)} with an argument
     * of {@link ResultChannelName#WARNINGS}.
     * 
     * @return true if warnings should be logged (default), false if not.
     */
//	public boolean getLogResultWarnings()
//	{
//		return logResultWarnings;
//	}

    /**
     * Indcates whether any warnings sent on the {@link ResultChannelName#WARNINGS} channel should
     * be automatically sent to the in-scope Logger object. Either way, any warnings sent will also
     * be available by calling {@link ResultSequence#getChannel(ResultChannelName)} with an argument
     * of {@link ResultChannelName#WARNINGS}.
     * 
     * @param logResultWarnings
     *            Pass true to cause any warnings sent to be automatically logged, false to do
     *            nothing wth warnings.
     */
//	public void setLogResultWarnings (boolean logResultWarnings)
//	{
//		this.logResultWarnings = logResultWarnings;
//	}

    // -------------------------------------------------------

    /**
     * The currently set (or default if never set) {@link ResultSequence} buffer size. A size of
     * zero indicates that the implementation default will be used.
     * 
     * @return The currently set buffer size value.
     */
    public int getResultBufferSize() {
        return resultBufferSize;
    }

    /**
     * Set the suggested {@link ResultSequence} buffer size for this execution. This is a hint to
     * the implementation. If the requested size is not reasonable (too big or too small) or not
     * appropriate for the result, this hint may be ignored or constrained. In most cases, the
     * default setting should work well.
     * 
     * @param resultBufferSize
     *            The suggested buffer size to use when processing the result of the execution. A
     *            value of zero means that the implementation should use programmatic defaults.
     */
    public void setResultBufferSize(int resultBufferSize) {
        this.resultBufferSize = resultBufferSize;
    }

    // -------------------------------------------------------

    /**
     * Get the read timeout value (in milliseconds) for this options object.
     * 
     * @return The timeout setting (in milliseconds). A value of zero indicates no timeout. A value
     *         of -1 indicates that the default is being used.
     */
    public int getTimeoutMillis() {
        return timeoutMillis;
    }

    /**
     * <p>
     * Set the timeout value, in milliseconds, for the low-level connection. This is almost
     * certainly not what you want. You probably want the {@link #setRequestTimeLimit(int)} option
     * to set a limit on the amount of time a request is allowed to run.
     * </p>
     * <p>
     * This option sets a read timeout on the low-level connection. It is here for historical
     * reasons but should not be used by most users. Connections are managed automatically by XCC
     * and low-level reads do not necessarily correspond to calls you make to the XCC API.
     * </p>
     * <p>
     * If query results are cached (the default, see {@link #setCacheResult(boolean)}), then this
     * option will usually have no effect because all data is read immediately. For non-cached
     * result sequences, this timeout may come into play. XCC prebuffers the data it reads which
     * means that low-level reads can happen at unpredictable times.
     * </p>
     * <p>
     * It is generally preferrable to set timeout values on the XBDC appserver in the admin UI to
     * control conection timeouts. Setting this to a value other than zero or -1 can result in a
     * small performance hit. Don't use it unless you really, really know what you're doing.
     * </p>
     * <p>
     * The default value for this setting is -1, which indicates that a default value should be
     * used. The default is usually zero. A value of zero indicates no timeout.
     * </p>
     * 
     * @param timeoutMillis
     *            The number of milliseconds to wait before timing out a low-level socket read. Zero
     *            explicitly indicates no timeout, -1 indicates the default should be used (which is
     *            typically zero, but could have been set on the {@link Session} to some other
     *            value.
     * @see #setRequestTimeLimit(int)
     */
    public void setTimeoutMillis(int timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    /**
     * <p>
     * A user-defined, name value sent with the {@link Request} and logged on the server.
     * </p>
     * 
     * @return The currently set value, or null.
     * @see #getRequestName()
     */
    public String getRequestName() {
        return requestName;
    }

    /**
     * Set a user-defined, name value to be associated with the {@link Request}. The value provided
     * must be an alphanumeric string (letters and numbers only, must start with a letter). It will
     * be logged as the HTTP Referer header in the access log and may be set as the name of the
     * request while running in the server. This name may be visible in the MarkLogic Server admin
     * interface.
     * 
     * @param requestName
     *            A String or null to clear the name.
     * @throws IllegalArgumentException
     *             If the string provided is not a valid request name.
     */
    public void setRequestName(String requestName) {
        if (!validRequestName(requestName)) {
            throw new IllegalArgumentException("Not a valid request name: " + requestName);
        }

        this.requestName = requestName;
    }

    // validation for above method
    private boolean validRequestName(String requestName) {
        if (requestName == null) {
            return true;
        }

        String req = requestName.trim();

        if (!Character.isLetter(req.charAt(0))) {
            return false;
        }

        for (int i = 0; i < req.length(); i++) {
            if (!Character.isLetterOrDigit(req.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    /**
     * The (possibly null) {@link java.util.Locale} object associated with this instance.
     * 
     * @return An instance of {@link java.util.Locale} or null. A value of null indicates that the
     *         JVM default locale should be associated with the request.
     */
    public Locale getLocale() {
        return locale;
    }

    /**
     * Set (or clear) the {@link java.util.Locale} object to associate with this options instance.
     * 
     * @param locale
     *            A {@link java.util.Locale} object or null. Setting the locale to null indicates
     *            that the JVM default should be used.
     */
    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    /**
     * The (possibly null) {@link java.util.TimeZone} object associated with this instance.
     * 
     * @return An instance of {@link java.util.TimeZone} or null. A value of null indicates that the
     *         JVM default timezone should be associated with the request.
     */
    public TimeZone getTimeZone() {
        return timeZone;
    }

    /**
     * Set (or clear) the {@link java.util.TimeZone} object to associate with this options instance.
     * 
     * @param timeZone
     *            A {@link java.util.TimeZone} object or null. Setting the timezone to null
     *            indicates that the JVM default should be used.
     */
    public void setTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
    }

    /**
     * Returns the point-in-time timestamp value, if any, set in this object. If a point-in-time
     * value is associated with a {@link Request} it will affect the view of the content the query
     * will run against.
     * 
     * @return A {@link BigInteger} object, or null.
     */
    public BigInteger getEffectivePointInTime() {
        return effectivePointInTime;
    }

    /**
     * <p>
     * Set a point-in-time timestamp value. When a point-in-time timestamp is associated with a
     * {@link Request} it determines the visibility of content that a query runs against.
     * </p>
     * <p>
     * The latest point-in-time timestamp may be obtained from the server by invoking
     * {@link com.marklogic.xcc.Session#getCurrentServerPointInTime()}.
     * </p>
     * <p>
     * When a non-zero point-in-time is in effect, only read-only requests are allowed. Updates,
     * which modify the state of the contentbase, are not allowed. Point-in-time queries are
     * effectively running in the past and updates are always applied to the current state of the
     * contentbase.
     * </p>
     * <p>
     * Note that some queries with an effective point-in-time may throw an
     * {@link com.marklogic.xcc.exceptions.XQueryException} if deleted fragments needed to recreate
     * the effective state have been dropped by a subsequent merge. The MarkLogic Server
     * administrative interface contains controls to retain fragments newer than a specified
     * timestamp.
     * </p>
     * 
     * @param effectivePointInTime
     *            An instance of {@link BigInteger} or null to clear the effective timestamp. Note
     *            that a parameter with the value zero is equivalent to null.
     */
    public void setEffectivePointInTime(BigInteger effectivePointInTime) {
        this.effectivePointInTime = effectivePointInTime;
    }

    /**
     * Get the default XQuery version string to use as the default for the request, if any.
     * 
     * @return An XQuery version String, or null.
     * @since 4.0
     */
    public String getDefaultXQueryVersion() {
        return defaultXQueryVersion;
    }

    /**
     * Set the default XQuery version that should apply to this request. Setting a default
     * determines which version of XQuery to use in the absence of an explicit declaration within
     * the code. If this value is never set, or if it's set to null, the default XQuery version
     * selected for the XDBC app server will be used.
     * 
     * @param versionString
     *            An XQuery version String, or null. Valid XQuery version strings are "0.9-ml",
     *            "1.0-ml" and "1.0". Setting to null defaults to the app server configuration.
     * @since 4.0
     */
    public void setDefaultXQueryVersion(String versionString) {
        defaultXQueryVersion = versionString;
    }

    /**
     * Get the "soft" request time limit (in seconds) to apply to the submitted request. The default
     * value is -1, which means to apply the programmatic default.
     * 
     * @return A number of seconds, or -1.
     * @see #setRequestTimeLimit(int)
     */
    public int getRequestTimeLimit() {
        return requestTimeLimit;
    }

    /**
     * <p>
     * Set the "soft" time limit (in seconds) to apply to the submitted request. Passing -1 means
     * use the default, possibly inherited from a {@link com.marklogic.xcc.Session} object. A value
     * of zero indicates that the default value configured on the appserver should be used. Any
     * other positive number is interpreted as a number of seconds.
     * </p>
     * <p>
     * The request soft time limit may be set to any value less than or equal to the hard limit
     * (max-time-limit) set on the app server. The setting will apply only to the submitted request,
     * it does not affect any permanent app server settings.
     * </p>
     * 
     * @param requestTimeLimit
     *            A number of seconds, or -1 for the XCC programmatic default, or zero for the app
     *            server default.
     */
    public void setRequestTimeLimit(int requestTimeLimit) {
        this.requestTimeLimit = requestTimeLimit;
    }

    // -------------------------------------------------------------

//	public void includeResultChannel (ResultChannelName channel)
//	{
//		includedChannels.add (channel);
//	}
//
//	public Set getIncludedChannels()
//	{
//		return Collections.unmodifiableSet (includedChannels);
//	}
//
//	public void excludeResultChannel (ResultChannelName channel)
//	{
//		excludedChannels.remove (channel);
//	}
//
//	public Set getExcludedChannels()
//	{
//		return Collections.unmodifiableSet (excludedChannels);
//	}

    /**
     * <p>
     * Set the option values of this object to the effective values obtained by merging each of the
     * RequestOption objects in the array. Last, non-default value wins (starting at index 0). Any
     * remaining values that indicate a default are replaced with the appropriate value.
     * </p>
     * <p>
     * This method is primarily intended for internal use. In general, you should instantiate a new
     * RequestOptions object and set only those properties you want to explicitly override.
     * </p>
     * 
     * @param others
     *            An array of RequestOption objects whose values will be collapsed into this oject.
     */
    public void applyEffectiveValues(RequestOptions[] others) {
        for (int i = 0; i < others.length; i++) {
            RequestOptions other = others[i];

            if (other.maxAutoRetry != -1) {
                maxAutoRetry = other.maxAutoRetry;
            }
            if (other.autoRetryDelayMillis != -1) {
                autoRetryDelayMillis = other.autoRetryDelayMillis;
            }
            if (other.timeoutMillis != -1) {
                timeoutMillis = other.timeoutMillis;
            }
            if (other.requestName != null) {
                requestName = other.requestName;
            }
            if (other.locale != null) {
                locale = other.locale;
            }
            if (other.timeZone != null) {
                timeZone = other.timeZone;
            }
            if (other.effectivePointInTime != null) {
                effectivePointInTime = other.effectivePointInTime;
            }
            if (!other.defaultCacheResult) {
                cacheResult = other.cacheResult;
            }
            if (other.resultBufferSize != 0) {
                resultBufferSize = other.resultBufferSize;
            }
            if (other.defaultXQueryVersion != null) {
                defaultXQueryVersion = other.defaultXQueryVersion;
            }
            if (other.requestTimeLimit != -2) {
                requestTimeLimit = other.requestTimeLimit;
            }
            if (other.queryLanguage != null) {
                queryLanguage = other.queryLanguage;
            }

//			if ( ! other.defaultLogResultWarnings) {
//				logResultWarnings = other.logResultWarnings;
//			}

//			logResultWarnings = other.logResultWarnings;
        }

        if (maxAutoRetry == -1) {
            maxAutoRetry = getDefaultValue(MAX_RETRY_SYSTEM_PROPERTY, DEFAULT_MAX_AUTO_RETRY);
        }
        if (autoRetryDelayMillis == -1) {
            autoRetryDelayMillis = getDefaultValue(AUTO_RETRY_DELAY_SYSTEM_PROPERTY, DEFAULT_AUTO_RETRY_DELAY_MILLIS);
        }
        if (timeoutMillis == -1) {
            timeoutMillis = 0;
        }
        if (locale == null) {
            locale = Locale.getDefault();
        }
        if (timeZone == null) {
            timeZone = TimeZone.getDefault();
        }
        if (resultBufferSize == 0) {
            resultBufferSize = MultipartSplitter.DEF_BUFFER_SIZE;
        }
    }

    // ---------------------------------------------------------

    private int getDefaultValue(String propName, int defaultValue) {
        Integer value = Integer.getInteger(propName, null);

        return ((value == null) ? defaultValue : value.intValue());
    }

    public String getQueryLanguage() {
        return queryLanguage;
    }

    public void setQueryLanguage(String queryLanguage) {
        this.queryLanguage = queryLanguage;
    }
}
