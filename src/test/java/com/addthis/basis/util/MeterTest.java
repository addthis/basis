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
package com.addthis.basis.util;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MeterTest {
    enum Events {
        bytes_read, bytes_written
    };

    Meter<Events> meter;

    @Before
    public void setUp() {
        // create a new meter
        meter = new Meter<Events>(Events.values());
        //// set up some metrics that you want to track
        // sum of all puts to the read counter (total bytes read)
        meter.addCountMetric(Events.bytes_read, "totalBytesReceived");
        // max read size
        meter.addMaxMetric(Events.bytes_read, "maxReadSize");
        // average read size
        meter.addAverageMetric(Events.bytes_read, "avgReadSize");
        // rate metric = count / time since last reset (bytes read per second)
        meter.addRateMetric(Events.bytes_read, "inBPS");
    }

    @Test
    public void basicTest() {
        long[] reads = {100, 250, 99, 123, 1, 0};
        for (long read : reads) {
            meter.inc(Events.bytes_read, read);
        }
        assertEquals(Calc.average(reads), meter.getAverage(Events.bytes_read));
        assertEquals(Calc.sum(reads), meter.getCount(Events.bytes_read));
        meter.mark();
    }

}
