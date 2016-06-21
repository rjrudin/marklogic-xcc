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
package com.marklogic.xcc.types;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Duration class is a mutable class which implements the XdmDuration interface.
 */
public class Duration implements XdmDuration {
    private static final BigDecimal ZERO = new BigDecimal("0.0");
    private boolean negative = false;
    private int years = 0;
    private int months = 0;
    private int days = 0;
    private int hours = 0;
    private int minutes = 0;
    private BigDecimal seconds = ZERO;

    // ---------------------------------------------------------------

    /**
     * Construct a duration object, initialized to zero length.
     */
    public Duration() {
    }

    /**
     * Construct a duration object initialized from the given string in the format of a serialized
     * xs:duration item. Examples: P2Y3M141DT12H46M12.34S, -P92M, -P32M2DT0.2S.
     * 
     * @param duration
     *            An xs:duration value as defined by <a
     *            href="http://www.w3.org/TR/xmlschema-2/#duration>XML Schema Part 2: Datatypes</a>
     * @throws IllegalArgumentException
     *             If the given string is not a valid duration value.
     */
    public Duration(String duration) {
        parseDuration(duration);
    }

    /**
     * Construct a duration object by specifying the individual values.
     * 
     * @param negative
     *            Pass true if the duration should be neagtive, otherwise false.
     * @param years
     *            The number of years.
     * @param months
     *            The number of months.
     * @param days
     *            The number of days.
     * @param hours
     *            The number of hours.
     * @param minutes
     *            The number of minutes.
     * @param seconds
     *            The (possibly fractional) number of seconds. Note that this is specified by a
     *            {@link BigDecimal} object. If null, a value of zero is assumed.
     */
    public Duration(boolean negative, int years, int months, int days, int hours, int minutes, BigDecimal seconds) {
        this.negative = negative;
        this.years = years;
        this.months = months;
        this.days = days;
        this.hours = hours;
        this.minutes = minutes;
        this.seconds = (seconds == null) ? ZERO : seconds;
    }

    // -----------------------------------------------
    // XdmDuration interface

    public boolean isPositive() {
        return !negative;
    }

    public boolean isNegative() {
        return negative;
    }

    public int getYears() {
        return years;
    }

    public int getMonths() {
        return months;
    }

    public int getDays() {
        return days;
    }

    public int getHours() {
        return hours;
    }

    public int getMinutes() {
        return minutes;
    }

    public long getWholeSeconds() {
        return seconds.intValue();
    }

    public BigDecimal getSeconds() {
        return seconds;
    }

    // ------------------------------------------------

    /**
     * Sets the sign of this duration, either positive or negative.
     * 
     * @param negative
     *            If true, the duration is considered negative, otherwise it's positive.
     */
    public void setSign(boolean negative) {
        this.negative = negative;
    }

    /**
     * Sets the years value of this duration.
     * 
     * @param years
     *            the integer years value.
     */
    public void setYears(int years) {
        this.years = years;
    }

    /**
     * Sets the months value of this duration.
     * 
     * @param months
     *            the integer months value.
     */
    public void setMonths(int months) {
        this.months = months;
    }

    /**
     * Sets the days value of this duration.
     * 
     * @param days
     *            the integer days value.
     */
    public void setDays(int days) {
        this.days = days;
    }

    /**
     * Sets the hours value of this duration.
     * 
     * @param hours
     *            the integer hours value.
     */
    public void setHours(int hours) {
        this.hours = hours;
    }

    /**
     * Sets the minutes value of this duration.
     * 
     * @param minutes
     *            the integer minutes value.
     */
    public void setMinutes(int minutes) {
        this.minutes = minutes;
    }

    /**
     * Sets the seconds value of this duration.
     * 
     * @param seconds
     *            the integer seconds value.
     */
    public void setWholeSeconds(long seconds) {
        this.seconds = new BigDecimal("" + seconds);
    }

    // -------------------------------------------------------------------

    // -P2Y4M5DT3H5M42.057S

    private static final String DURATION_PATTERN = "(-)?P" + "((\\d+)Y)?" + "((\\d+)M)?" + "((\\d+)D)?" + "(T"
            + "((\\d+)H)?" + "((\\d+)M)?" + "((\\d*\\.?\\d*)S)?" + ")?";

    private static final int SIGN = 1;
    private static final int YEARS = 3;
    private static final int MONTHS = 5;
    private static final int DAYS = 7;
    private static final int HOURS = 10;
    private static final int MINUTES = 12;
    private static final int SECONDS = 14;

    private static final Pattern durPattern = Pattern.compile(DURATION_PATTERN);

    private void parseDuration(String dur) {
        Matcher matcher = durPattern.matcher(dur);

        if (!matcher.matches()) {
            throw new IllegalArgumentException("Not a valid duration: " + dur);
        }

        negative = "-".equals(matcher.group(SIGN));
        years = intValue(matcher.group(YEARS));
        months = intValue(matcher.group(MONTHS));
        days = intValue(matcher.group(DAYS));
        hours = intValue(matcher.group(HOURS));
        minutes = intValue(matcher.group(MINUTES));
        seconds = bigDecimalValue(matcher.group(SECONDS));
    }

    private int intValue(String strValue) {
        if ((strValue == null) || (strValue.length() == 0)) {
            return (0);
        }

        return Integer.parseInt(strValue);
    }

    private BigDecimal bigDecimalValue(String strValue) {
        if ((strValue == null) || (strValue.length() == 0)) {
            return (ZERO);
        }

        return new BigDecimal(strValue);
    }

    // -----------------------------------------------------------------

    /**
     * Compares this {@link XdmDuration} object to the specified object. The result is true if and
     * only if the argument is not null and the sign, year, month, day, hours, minutes, seconds and
     * subseconds values have the same value as this object.
     * 
     * @param otherObj
     *            the Duration object to compare
     * @return true if the objects have the same duration value, false otherwise.
     */
    @Override
    public boolean equals(Object otherObj) {
        if (otherObj == null) {
            return false;
        }

        if (!(otherObj instanceof XdmDuration)) {
            return false;
        }

        XdmDuration other = (XdmDuration)otherObj;

        return ((other.isNegative() == this.negative) && (other.getYears() == this.years)
                && (other.getMonths() == this.months) && (other.getDays() == this.days)
                && (other.getHours() == this.hours) && (other.getMinutes() == this.minutes) && (other.getSeconds()
                .equals(this.seconds)));
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    /**
     * Formats this duration object as a String in the format prescribed in the W3C description of
     * XML datatypes for a duration.
     * <p/>
     * Example: -P2Y4M5DT3H5M42.057S
     * <p/>
     * Represents a duration of minus 2 years, 4 months, 5 days, 3 hours, 5 minutes, and 42.057
     * seconds.
     * 
     * @return the String representation of the duration value.
     * @see <a href="http://www.w3.org/TR/xmlschema-2/#duration>XML Schema Part 2: Datatypes</a>
     */
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();

        if (negative) {
            sb.append("-");
        }

        sb.append("P");

        // Degenerate case.
        if ((years == 0) && (months == 0) && (days == 0) && (hours == 0) && (minutes == 0) && (seconds.equals(ZERO))) {
            sb.append("T0S");

            return (sb.toString());
        }

        if (years != 0) {
            sb.append(String.valueOf(years)).append("Y");
        }

        if (months != 0) {
            sb.append(String.valueOf(months)).append("M");
        }

        if (days != 0) {
            sb.append(String.valueOf(days)).append("D");
        }

        if ((hours != 0) || (minutes != 0) || (!seconds.equals(ZERO))) {
            sb.append("T");
        }

        if (hours != 0) {
            sb.append(String.valueOf(hours)).append("H");
        }

        if (minutes != 0) {
            sb.append(String.valueOf(minutes)).append("M");
        }

        if (!seconds.equals(ZERO)) {
            BigDecimal intVal = new BigDecimal(seconds.toBigInteger());

            if (seconds.compareTo(intVal) == 0) {
                sb.append(intVal);
            } else {
                sb.append(seconds);
            }

            sb.append("S");
        }

        return sb.toString();
    }
}
