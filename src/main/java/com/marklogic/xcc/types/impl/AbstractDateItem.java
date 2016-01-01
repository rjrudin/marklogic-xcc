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
package com.marklogic.xcc.types.impl;

import java.math.BigDecimal;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import com.marklogic.xcc.types.ItemType;

abstract public class AbstractDateItem extends AbstractStringItem {
    protected static final String GDAY_FMT_STRING = "---dd";
    protected static final String GMONTH_FMT_STRING = "--MM";
    protected static final String GMONTHDAY_FMT_STRING = "--MM-dd";
    protected static final String GYEAR_FMT_STRING = "yyyy";
    protected static final String GYEARMONTH_FMT_STRING = "yyyy-MM";
    protected static final String DATE_FMT_STRING = "yyyy-MM-dd";
    protected static final String DATETIME_FMT_STRING = "yyyy-MM-dd'T'HH:mm:ss";
    protected static final String TIME_FMT_STRING = "HH:mm:ss";

    private final TimeZone timezone;
    private final Locale locale;

    public AbstractDateItem(ItemType type, String value, TimeZone timezone, Locale locale) {
        super(type, value);

        this.timezone = (timezone == null) ? TimeZone.getDefault() : timezone;
        this.locale = (locale == null) ? Locale.getDefault() : locale;
    }

    // -----------------------------------------------------------

    protected Date dateFromDateString(String str) {
        return (dateFromString(str, DATE_FMT_STRING, timezone, locale));
    }

    protected Date dateFromDateTimeString(String str) {
        return (dateFromString(str, DATETIME_FMT_STRING, timezone, locale));
    }

    protected Date dateFromTimeString(String str) {
        return (dateFromString(str, TIME_FMT_STRING, timezone, locale));
    }

    // TODO: fully test Timezone and locale variations
    private Date dateFromString(String str, String fmt, TimeZone tz, Locale locale) {
        String val = str.trim();

        SimpleDateFormat sdf = new SimpleDateFormat(fmt, locale);
        sdf.setTimeZone(tz);

        ParsePosition pp = new ParsePosition(0);
        Date date = sdf.parse(val, pp);

        if (date == null) {
            throw new IllegalArgumentException("Not a valid date/time string: " + str + " (" + fmt + ")");
        }

        int millis = 0;

        if (pp.getIndex() < val.length() && val.charAt(pp.getIndex()) == '.') {
            pp.setIndex(pp.getIndex() + 1);
            int end = pp.getIndex();

            while (end < val.length() && val.charAt(end) >= '0' && val.charAt(end) <= '9') {
                end++;
            }

            millis = millisFromFractional(val.substring(pp.getIndex(), end));
            pp.setIndex(end);
        }

        if (pp.getIndex() < val.length()) {
            if (val.charAt(pp.getIndex()) == 'Z') {
                tz = TimeZone.getTimeZone("UTC");
            } else {
                tz = TimeZone.getTimeZone("GMT" + val.substring(pp.getIndex()));
            }
        }

        GregorianCalendar cal = new GregorianCalendar(tz, locale);

        cal.setTime(date);
        cal.set(Calendar.MILLISECOND, millis);
        date = cal.getTime();

        return date;
    }

    private static final BigDecimal oneThousand = new BigDecimal("1000");

    // package local for unit testing
    static int millisFromFractional(String fraction) {
        BigDecimal d = new BigDecimal("0." + fraction);
        d = d.setScale(3, BigDecimal.ROUND_HALF_UP);
        d = d.multiply(oneThousand);

        return d.intValue();
    }

    // -----------------------------------------------------------

    protected GregorianCalendar gCalFromGDayString(String str) {
        return (gCalFromString(str, GDAY_FMT_STRING, timezone, locale));
    }

    protected GregorianCalendar gCalFromGMonthString(String str) {
        return (gCalFromString(str, GMONTH_FMT_STRING, timezone, locale));
    }

    protected GregorianCalendar gCalFromGMonthDayString(String str) {
        return (gCalFromString(str, GMONTHDAY_FMT_STRING, timezone, locale));
    }

    protected GregorianCalendar gCalFromGYearString(String str) {
        return (gCalFromString(str, GYEAR_FMT_STRING, timezone, locale));
    }

    protected GregorianCalendar gCalFromGYearMonthString(String str) {
        return (gCalFromString(str, GYEARMONTH_FMT_STRING, timezone, locale));
    }

    // TODO: Test timezone and locale settings
    private GregorianCalendar gCalFromString(String str, String fmt, TimeZone tz, Locale locale) {
        String val = str.trim();

        SimpleDateFormat sdf = new SimpleDateFormat(fmt, locale);
        sdf.setTimeZone(tz);

        ParsePosition pp = new ParsePosition(0);
        Date d = sdf.parse(val, pp);

        if (d == null) {
            throw new IllegalArgumentException("Not a valid Gregorian string: " + str + " (" + fmt + ")");
        }

        if (pp.getIndex() < val.length()) {
            tz = TimeZone.getTimeZone("GMT" + val.substring(pp.getIndex()));
        }

        GregorianCalendar gcal = new GregorianCalendar(tz, locale);
        gcal.setTime(d);

        return gcal;
    }

    // ------------------------------------------------------------
}
