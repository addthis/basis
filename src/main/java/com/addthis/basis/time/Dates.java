/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.addthis.basis.time;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;


public class Dates {
    public static final DateTimeFormatter rfc1123 = DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss Z");
    public static final DateTimeFormatter yMdFormat = DateTimeFormat.forPattern("yyyy-MM-dd");

    /**
     * Convenience method to create a new single field period of arbitrary type
     * (e.g. 10 days, 6 months, etc.)
     * <p/>
     * Requires that type be a single field period
     *
     * @param length
     * @param type
     * @return
     */
    public static Period period(int length, PeriodType type) {
        return new Period(null, type).withField(type.getFieldType(0), length);
    }

    /**
     * Truncate interval start and end by current time
     * (start/end values after current time are set to current time)
     * <p/>
     * Truncate interval start by a specified number of period types
     * (eg. 30 days, 52 weeks, etc.)
     * <p/>
     * If type is null, no truncation is performed.
     * <p/>
     * When no truncation is performed, the input interval is returned
     * (this is useful for efficiently testing if truncation was performed).
     *
     * @param interval
     * @param limit    number of TYPE periods above which interval should be truncated
     * @param type     single field period type (the result of this method is undefined for multi-field period types)
     * @return
     */
    public static Interval truncateInterval(Interval interval, int limit, PeriodType type) {
        Interval truncatedInterval = interval;
        if (type != null) {
            // Truncate end
            DateTime now = new DateTime();
            if (interval.getEnd().isAfter(now)) {
                if (interval.getStart().isAfter(now)) {
                    truncatedInterval = new Interval(now, now);
                } else {
                    truncatedInterval = interval.withEnd(now);
                }
            }

            // Truncate start
            if (truncatedInterval.toPeriod(type).getValue(0) > --limit) {
                Period limitPeriod = period(limit, type);
                DateTime truncatedStart = truncatedInterval.getEnd().minus(limitPeriod);
                truncatedInterval = truncatedInterval.withStart(truncatedStart);
            }
        }
        return truncatedInterval;
    }

    /**
     * Create an interval from two date strings
     * <p/>
     * begStr | endStr | return
     * -------------------------
     * bad   |  bad   | (now, now)
     * bad   |   E    | (E, E)
     * B    |  bad   | (B, B)
     * B    |   E    | (B, E)
     * B(>E)|   E(<B)| (E, E)
     * <p/>
     * ("bad" in the table above indicates that the input
     * is either null or fails to parse)
     * <p/>
     * TODO: accept default beg/end parameters
     *
     * @param begStr beginning of interval
     * @param endStr end of interval
     * @return
     */
    public static Interval parseInterval(String begStr, String endStr, DateTimeFormatter format) {
        DateTime beg = null;
        DateTime end = null;
        boolean begFailed = false;
        boolean endFailed = false;

        try {
            beg = format.parseDateTime(begStr);
        } catch (Exception e) {
            begFailed = true;
        }

        try {
            end = format.parseDateTime(endStr);
        } catch (Exception e) {
            endFailed = true;
        }


        if (begFailed && endFailed) {
            end = beg = new DateTime();
        } else if (begFailed) {
            beg = end;
        } else if (endFailed) {
            end = beg;
        }

        if (beg.isAfter(end)) {
            beg = end;
        }

        return new Interval(beg, end);
    }

    /**
     * Create an interval from two date strings in yyyy-MM-dd format
     *
     * @param begStr
     * @param endStr
     * @return
     */
    public static Interval parseInterval(String begStr, String endStr) {
        return parseInterval(begStr, endStr, yMdFormat);
    }


    /*
     * Java Date methods
     * (deprecated - use Joda time and std formats)
     */

    public static final DateTimeFormatter yMdHFormat = DateTimeFormat.forPattern("yyMMddHH");

    /**
     * @param d
     * @return Date with same date as d and time 23:59:59:999
     */
    public static Date endOfDay(Date d) {
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(d);
        calendar.set(Calendar.AM_PM, Calendar.PM);
        calendar.set(Calendar.MILLISECOND, calendar.getActualMaximum(Calendar.MILLISECOND));
        calendar.set(Calendar.SECOND, calendar.getActualMaximum(Calendar.SECOND));
        calendar.set(Calendar.MINUTE, calendar.getActualMaximum(Calendar.MINUTE));
        calendar.set(Calendar.HOUR, calendar.getActualMaximum(Calendar.HOUR));
        return calendar.getTime();
    }

    /**
     * @param d
     * @return Date with same date as d and time 00:00:00:000
     */
    public static Date begOfDay(Date d) {
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(d);
        calendar.set(Calendar.AM_PM, Calendar.AM);
        calendar.set(Calendar.MILLISECOND, calendar.getActualMinimum(Calendar.MILLISECOND));
        calendar.set(Calendar.SECOND, calendar.getActualMinimum(Calendar.SECOND));
        calendar.set(Calendar.MINUTE, calendar.getActualMinimum(Calendar.MINUTE));
        calendar.set(Calendar.HOUR, calendar.getActualMinimum(Calendar.HOUR));
        return calendar.getTime();
    }

    /**
     * For iterating over the period units in an interval (inclusive of start and end)
     * <p/>
     * e.g. iterating over [2008-01-01, 2008-01-03] by DAYS will result in a series of
     * DateTime objects which when formatted as above produce the following:
     * 2008-01-01, 2008-01-02, 2008-01-03
     * regardless of the time fields in the input interval
     *
     * @param interval
     * @return
     */
    public static Iterable<DateTime> iterableInterval(Interval interval, DTimeUnit type) {
        return new IterableInterval(interval, type);
    }

    public static Iterator<DateTime> intervalIterator(Interval interval, DTimeUnit type) {
        return new IterableInterval(interval, type);
    }

    private static class IterableInterval implements Iterable<DateTime>, Iterator<DateTime> {
        private DateTime end;
        private DateTime currentInstant;
        private Period increment;

        public IterableInterval(Interval interval, DTimeUnit per) {
            if (per == DTimeUnit.ALL_TIME) {
                this.increment = period(1, PeriodType.millis());
                this.end = interval.getStart();
                this.currentInstant = end.minus(increment);
            } else {
                this.increment = period(1, per.toPeriodType());
                this.end = interval.getEnd().property(per.toDateTimeFieldType()).roundFloorCopy();
                this.currentInstant = interval.getStart().minus(increment);
            }
        }

        public Iterator<DateTime> iterator() {
            return this;
        }

        /* (non-Javadoc)
         * @see java.util.Iterator#hasNext()
         */
        public boolean hasNext() {
            return currentInstant.isBefore(end);
        }

        /* (non-Javadoc)
         * @see java.util.Iterator#next()
         */
        public DateTime next() {
            currentInstant = currentInstant.plus(increment);
            return currentInstant;
        }

        /* (non-Javadoc)
         * @see java.util.Iterator#remove()
         */
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
