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

import java.util.concurrent.atomic.AtomicLong;

/**
 * A class for simple event per second benchmarking
 */
public final class Bench {
    public Bench() {
        this(new String[]{"tick"}, 1000);
    }

    public Bench(String name, long rate) {
        this(new String[]{name}, rate);
    }

    @SuppressWarnings("unchecked")
    public Bench(java.util.EnumSet set, long rate) {
        Enum e[] = new Enum[set.size()];
        set.toArray(e);
        String nm[] = new String[e.length];
        for (int i = 0; i < nm.length; i++) {
            nm[i] = e[i].name();
        }
        setNames(nm);
        setPeriod(rate);
        mark();
        marks = 0;
    }

    public Bench(String names[], long rate) {
        setNames(names);
        setPeriod(rate);
        mark();
        marks = 0;
    }

    private long mark;
    private long rate;
    private String names[];
    private AtomicLong counts[];
    private long rates[];
    private long marks;

    public void setNames(String name[]) {
        assert (name != null && name.length > 0);
        this.names = name;
        this.rates = new long[name.length];
        this.counts = new AtomicLong[name.length];
        for (int i = 0; i < name.length; i++) {
            counts[i] = new AtomicLong(0);
        }
    }

    public void setPeriod(long rate) {
        this.rate = rate;
    }

    public long getPeriod() {
        return rate;
    }

    public long getMarkCalls() {
        return marks;
    }

    /**
     * Set event count to max of existing and new.
     *
     * @param which
     * @param ev
     * @return
     */
    public synchronized long maxEvents(int which, long ev) {
        counts[which].set(Math.max(counts[which].get(), ev));
        return counts[which].get();
    }

    public long maxEvents(Enum which, long ev) {
        return maxEvents(which.ordinal(), ev);
    }

    /**
     * Add X events to the event count.
     *
     * @param which
     * @param ev
     * @return
     */
    public long addEvents(int which, long ev) {
        return counts[which].getAndAdd(ev);
    }

    public long addEvents(Enum which, long ev) {
        return addEvents(which.ordinal(), ev);
    }

    public synchronized long deltaEvents(int which, long ev) {
        rates[which] = -1;
        long ret = ev - counts[which].get();
        counts[which].set(ev);
        return ret;
    }

    public long deltaEvents(Enum which, long ev) {
        return deltaEvents(which.ordinal(), ev);
    }

    /**
     * Returns the number of events per second since the last mark.
     * Resets the event counter;
     *
     * @return events per time period for the first tracked item
     */
    public synchronized long mark() {
        long time = System.currentTimeMillis();
        for (int i = 0; i < names.length; i++) {
            if (rates[i] >= 0) {
                rates[i] = (counts[i].get() * rate) / (time - mark + 1);
                counts[i].set(0);
            }
        }
        mark = time;
        marks++;
        return rates[0];
    }

    public long getLastTime() {
        return mark;
    }

    public long getEventCount(int which) {
        return counts[which].get();
    }

    public long getEventCount(Enum which) {
        return getEventCount(which.ordinal());
    }

    public long getEventRate(int which) {
        assert (which >= 0 && which < names.length);
        return rates[which];
    }

    public long getEventRate(Enum which) {
        return getEventRate(which.ordinal());
    }

    public long getPeriodRemaining() {
        return (mark + rate) - System.currentTimeMillis();
    }

    public boolean periodHasElapsed() {
        return hasElapsed(rate);
    }

    public boolean hasElapsed(long time) {
        return sinceLastMark() >= time;
    }

    public long sinceLastMark() {
        return System.currentTimeMillis() - mark;
    }
}
