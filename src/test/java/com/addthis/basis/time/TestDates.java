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

import org.joda.time.DateTime;
import org.joda.time.DateTimeFieldType;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;
import org.joda.time.DurationFieldType;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.junit.Ignore;
import org.junit.Test;

import static com.addthis.basis.time.Dates.rfc1123;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestDates {
    private DTimeUnit[] types = DTimeUnit.values();

    @Test
    public void testIterableInterval() {
        for (DTimeUnit unit : types) {
            switch (unit) {
                case ALL_TIME:
                default:
                    testIterableInterval(0, unit);
                    testIterableInterval(1, unit);
                    testIterableInterval(2, unit);
                    testIterableInterval(100, unit);
            }
        }
    }

    @SuppressWarnings("unused")
    private void testIterableInterval(int length, DTimeUnit unit) {
        int expectedCount;
        DateTimeFieldType dtft = null;
        PeriodType pt = null;

        if (unit == DTimeUnit.ALL_TIME) {
            dtft = DateTimeFieldType.dayOfMonth();
            pt = PeriodType.days();
            expectedCount = 1;
        } else {
            dtft = unit.toDateTimeFieldType();
            pt = unit.toPeriodType();
            expectedCount = length + 1;
        }

        Period unitPeriod = Dates.period(1, pt);

        DateTime begEarly = new DateTime().property(dtft).roundFloorCopy();
        DateTime begLate = begEarly.plus(unitPeriod).minusMillis(1);
        DateTime endEarly = begEarly.plus(Dates.period(length, pt));
        DateTime endLate = endEarly.plus(unitPeriod).minusMillis(1);

        Interval interval = new Interval(begEarly, endEarly);
        int count = 0;
        for (DateTime dt : Dates.iterableInterval(interval, unit)) {
            count++;
        }
        assertEquals(interval.toString(), expectedCount, count);

        interval = new Interval(begEarly, endLate);
        count = 0;
        for (DateTime dt : Dates.iterableInterval(interval, unit)) {
            count++;
        }
        assertEquals(interval.toString(), expectedCount, count);

        interval = new Interval(begLate, endLate);
        count = 0;
        for (DateTime dt : Dates.iterableInterval(interval, unit)) {
            count++;
        }
        assertEquals(interval.toString(), expectedCount, count);

        if (length > 0) {
            interval = new Interval(begLate, endEarly);
            count = 0;
            for (
                    DateTime dt : Dates.iterableInterval(interval, unit)) {
                count++;
            }
            assertEquals(interval.toString(), expectedCount, count);
        }
    }

    @Test
    public void testTruncateInterval() {
        DateTime end = new DateTime();
        Interval interval;

        for (DTimeUnit per : types) {
            switch (per) {
                case ALL_TIME:
                    break;
                default:
                    PeriodType type = per.toPeriodType();
                    DurationFieldType fieldType = type.getFieldType(0);

                    // instant (no truncation)
                    interval = new Interval(Dates.period(0, type), end);
                    interval = Dates.truncateInterval(interval, 10, type);
                    assertEquals(interval.toString(), 0, interval.toPeriod(type).get(fieldType));

                    // interval does not exceed limit (no truncation)
                    interval = new Interval(Dates.period(5, type), end);
                    interval = Dates.truncateInterval(interval, 10, type);
                    assertEquals(interval.toString(), 5, interval.toPeriod(type).get(fieldType));

                    // interval exceeds limit
                    interval = new Interval(Dates.period(10, type), end);
                    interval = Dates.truncateInterval(interval, 10, type);
                    assertEquals(interval.toString() + ',' + type.getName(), 9, interval.toPeriod(type).get(fieldType));

                    // null (no truncation)
                    interval = new Interval(Dates.period(100, type), end);
                    interval = Dates.truncateInterval(interval, 10, null);
                    assertEquals(interval.toString(), 100, interval.toPeriod(type).get(fieldType));

                    // end is in the future
                    interval = new Interval(Dates.period(11, type), (new DateTime()).plus(Dates.period(10, type)));
                    interval = Dates.truncateInterval(interval, 5, type);
                    assertFalse(interval.getEnd().isAfterNow());
                    assertEquals(interval.toString(), 1, interval.toPeriod(type).get(fieldType));

                    // end is in the future and limit exceeded
                    interval = new Interval(Dates.period(20, type), (new DateTime()).plus(Dates.period(10, type)));
                    interval = Dates.truncateInterval(interval, 5, type);
                    assertFalse(interval.getEnd().isAfterNow());
                    assertEquals(interval.toString(), 4, interval.toPeriod(type).get(fieldType));

                    // start and end are in the future
                    interval = new Interval((new DateTime()).plus(Dates.period(5, type)), (new DateTime()).plus(Dates.period(10, type)));
                    interval = Dates.truncateInterval(interval, 3, type);
                    assertFalse(interval.getEnd().isAfterNow());
                    assertFalse(interval.getEnd().isBefore(end));
                    assertEquals(interval.getStart(), interval.getEnd());
            }
        }
    }

    @Test
    public void testParseInterval() {
        DateTime now = new DateTime();
        DateTime beg = now.minusMonths(2).withTimeAtStartOfDay();
        DateTime end = now.plusMonths(2).withTimeAtStartOfDay();
        String begStr = beg.toString(Dates.yMdFormat);
        String endStr = end.toString(Dates.yMdFormat);
        String invStr = "DEADBEEF";
        Interval nowInterval = new Interval(now, now.plusSeconds(30));

        // null, null
        assertTrue(nowInterval.contains(Dates.parseInterval(null, null)));

        // null, E
        assertEquals(new Interval(end, end), Dates.parseInterval(null, endStr));

        // B, null
        assertEquals(new Interval(beg, beg), Dates.parseInterval(begStr, null));

        // B, E
        assertEquals(new Interval(beg, end), Dates.parseInterval(begStr, endStr));

        // E, B
        assertEquals(new Interval(beg, beg), Dates.parseInterval(endStr, begStr));

        // inv, inv
        assertTrue(nowInterval.contains(Dates.parseInterval(invStr, invStr)));

        // inv, E
        assertEquals(new Interval(end, end), Dates.parseInterval(invStr, endStr));

        // B, inv
        assertEquals(new Interval(beg, beg), Dates.parseInterval(begStr, invStr));

        // inv, null
        assertTrue(nowInterval.contains(Dates.parseInterval(invStr, null)));

        // null, inv
        assertTrue(nowInterval.contains(Dates.parseInterval(null, invStr)));

    }

    @Test
    @Ignore
    public void testJoda() {
        DateTime begEarly = new DateTime().withTimeAtStartOfDay();
        DateTime begNoon = begEarly.withHourOfDay(12);
        DateTime begLate = begEarly.withHourOfDay(23);

        DateTime endEarly = begEarly.plusDays(2);
        DateTime endNoon = endEarly.withHourOfDay(12);
        DateTime endLate = endEarly.withHourOfDay(23);

        assertEquals(2, Days.daysBetween(begEarly, endEarly).getDays());
        assertEquals(2, Days.daysBetween(begEarly, endNoon).getDays());
        assertEquals(2, Days.daysBetween(begEarly, endLate).getDays());

        assertEquals(1, Days.daysBetween(begNoon, endEarly).getDays());
        assertEquals(2, Days.daysBetween(begNoon, endNoon).getDays());
        assertEquals(2, Days.daysBetween(begNoon, endLate).getDays());

        assertEquals(1, Days.daysBetween(begLate, endEarly).getDays());
        assertEquals(1, Days.daysBetween(begLate, endNoon).getDays());
        assertEquals(2, Days.daysBetween(begLate, endLate).getDays());

        Interval interval = new Interval(null);
        assertTrue(interval.toString(), interval.getStart().equals(interval.getEnd()));
    }

    @Test
    public void testRfc1123() {
        DateTime dt = new DateTime(0, DateTimeZone.UTC);
        assertEquals("Thu, 01 Jan 1970 00:00:00 +0000", dt.toString(rfc1123));

        dt = new DateTime(2050, 12, 26, 11, 33, 8, 0, DateTimeZone.UTC);
        assertEquals("Mon, 26 Dec 2050 11:33:08 +0000", dt.toString(rfc1123));

        dt = new DateTime(2050, 12, 26, 11, 33, 8, 0, DateTimeZone.forID("EST"));
        assertEquals("Mon, 26 Dec 2050 11:33:08 -0500", dt.toString(rfc1123));
    }
}
