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

import org.joda.time.DateTimeFieldType;
import org.joda.time.DurationFieldType;
import org.joda.time.PeriodType;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;

public enum DTimeUnit {
    ALL_TIME(null, // PeriodType does not support eras, must handle "all time" as a special case
            null, // TODO: consider developing AllTime classes for PeriodType, DurationFieldType, DateTimeFieldType
            null,
            new DateTimeFormatterBuilder().appendLiteral("All Time").toFormatter()),

    MINUTE(PeriodType.minutes(),
            DurationFieldType.minutes(),
            DateTimeFieldType.minuteOfHour(),
            DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm")),

    HOUR(PeriodType.hours(),
            DurationFieldType.hours(),
            DateTimeFieldType.hourOfDay(),
            DateTimeFormat.forPattern("yyyy-MM-dd'T'HH")),

    LAST24(PeriodType.minutes(),
            DurationFieldType.minutes(),
            DateTimeFieldType.minuteOfHour(),
            DateTimeFormat.forPattern("yyyy-MM-dd")),

    DAY(PeriodType.days(),
            DurationFieldType.days(),
            DateTimeFieldType.dayOfMonth(),
            DateTimeFormat.forPattern("yyyy-MM-dd")),

    WEEK(PeriodType.weeks(),
            DurationFieldType.weeks(),
            DateTimeFieldType.weekOfWeekyear(),
            DateTimeFormat.forPattern("xxxx-'W'ww")),

    MONTH(PeriodType.months(),
            DurationFieldType.months(),
            DateTimeFieldType.monthOfYear(),
            DateTimeFormat.forPattern("yyyy-MM"));


    private DateTimeFieldType dateTimeFieldType;
    private DurationFieldType durationFieldType;
    private PeriodType periodType;
    private DateTimeFormatter format;

    DTimeUnit(PeriodType periodType, DurationFieldType durationFieldType, DateTimeFieldType dateTimeFieldType, DateTimeFormatter format) {
        this.periodType = periodType;
        this.durationFieldType = durationFieldType;
        this.dateTimeFieldType = dateTimeFieldType;
        this.format = format;
    }

    public DurationFieldType toDurationFieldType() {
        return durationFieldType;
    }

    /**
     * @return
     */
    public PeriodType toPeriodType() {
        return periodType;
    }

    /**
     * @return
     */
    public DateTimeFieldType toDateTimeFieldType() {
        return dateTimeFieldType;
    }

    public DateTimeFormatter getFormat() {
        return format;
    }
}
