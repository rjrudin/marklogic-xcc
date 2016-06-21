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
package com.marklogic.io;

import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Formatter;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

/**
 * A configurable Log message formatter that prepends messages with time, level, thread and location
 * information. The date/time format is configurable and the four meta-message attributes can be
 * disabled if desired.
 */
public class LogFormatter extends Formatter {
    private static final String nl = System.getProperty("line.separator");
    private static final String DEFUALT_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

    private final SimpleDateFormat fmt;
    private final StringBuffer dateSb = new StringBuffer(512);
    private final FieldPosition fpos = new FieldPosition(0);
    private final StringBuffer sb = new StringBuffer(512);
    private final boolean showTime;
    private final boolean showLevel;
    private final boolean showThread;
    private final boolean showLocation;

    public LogFormatter(Properties props, LogManager lm) {
        String className = getClass().getName();

        fmt = new SimpleDateFormat(getProperty(props, lm, className + ".time-format", DEFUALT_TIME_FORMAT));
        showTime = getBoolean(props, lm, className + ".show-time", true);
        showLevel = getBoolean(props, lm, className + ".show-level", true);
        showThread = getBoolean(props, lm, className + ".show-thread", true);
        showLocation = getBoolean(props, lm, className + ".show-location", true);
    }

    public LogFormatter() {
        this(null, LogManager.getLogManager());
    }

    @Override
    public synchronized String format(LogRecord record) {
        String pad = "";

        sb.setLength(0);

        if (showTime) {
            sb.append(pad).append(formatTime(record));
            pad = " ";
        }

        if (showLevel) {
            sb.append(pad).append(record.getLevel());
            pad = " ";
        }

        if (showThread) {
            sb.append(pad).append("[").append(record.getThreadID()).append("]");
            pad = " ";
        }

        if (showLocation) {
            sb.append(pad).append("(").append(formatLocation(record)).append(")");
            pad = " ";
        }

        sb.append(": ").append(record.getMessage()).append(nl);

        Throwable t = record.getThrown();

        if (t == null) {
            return (sb.toString());
        }

        StackTraceElement[] elements = t.getStackTrace();

        sb.append(" ").append(t.toString()).append(nl);

        for (int i = 0; i < elements.length; i++) {
            StackTraceElement element = elements[i];

            sb.append("\t").append(element.toString()).append(nl);
        }

        return (sb.toString());
    }

    private String formatLocation(LogRecord record) {
        String className = record.getSourceClassName();
        int idx = className.lastIndexOf(".");

        if (idx != -1) {
            className = className.substring(idx + 1);
        }

        return (className + "." + record.getSourceMethodName());
    }

    private String formatTime(LogRecord record) {
        dateSb.setLength(0);

        return (fmt.format(new Date(record.getMillis()), dateSb, fpos).toString());
    }

    private boolean getBoolean(Properties props, LogManager lm, String property, boolean defaultValue) {
        String value = getProperty(props, lm, property, null);

        if (value == null) {
            return defaultValue;
        }

        try {
            return Boolean.valueOf(value).booleanValue();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private String getProperty(Properties props, LogManager lm, String property, String defaultValue) {
        String value = (props == null) ? null : props.getProperty(property);

        if (value != null) {
            return value;
        }

        value = lm.getProperty(property);

        if (value != null) {
            return value;
        }

        return defaultValue;
    }
}
