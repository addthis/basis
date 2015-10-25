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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A utility for easy counting of events. This class introduces two concepts:
 * <ul>
 * <li>Counters against which you can tally events</li>
 * <li>Metrics like count, average, rate, etc, which are computed from those
 * counters</li>
 * </ul>
 * Meter can store multiple counters (ex: hits, errors) which you define in an
 * Enum and pass to the constructor. Once you've instantiated a Meter, set up
 * the metrics you want to track and start counting. Call peek() at any time to
 * retrieve metrics or call mark() to reset the counters.
 *
 * @param <E> Enum type that defines a set of counters
 */
public class Meter<E extends Enum<E>> {
    private Counter[] counters; // metrics counted since last mark()
    private Metric[] metrics; // metric labels for each counter

    /**
     * @param counters initialize with enum.values()
     */
    public Meter(E[] counters) {
        this.counters = new Counter[counters.length];
        for (int i = 0; i < counters.length; i++) {
            this.counters[i] = new Counter();
        }
        metrics = new Metric[this.counters.length];
        for (int i = 0; i < metrics.length; i++) {
            metrics[i] = new Metric();
        }
    }

    /**
     * add a count metric for a counter
     *
     * @param which the counter
     * @param label label to use for reporting
     */
    public Meter<E> addCountMetric(E which, String label) {
        metrics[which.ordinal()].setCountLabel(label);
        return this;
    }

    /**
     * add a count metric for a counter
     *
     * @param which the counter
     */
    public Meter<E> addCountMetric(E which) {
        return addCountMetric(which, which.toString());
    }

    /**
     * add a max metric for a counter
     *
     * @param which the counter
     * @param label label to use for reporting
     */
    public Meter<E> addMaxMetric(E which, String label) {
        metrics[which.ordinal()].setMaxLabel(label);
        return this;
    }

    /**
     * add a max metric for a counter
     *
     * @param which the counter
     */
    public Meter<E> addMaxMetric(E which) {
        return addMaxMetric(which, which.toString());
    }

    /**
     * add an average metric for a counter
     *
     * @param which the counter
     * @param label label to use for reporting
     */
    public Meter<E> addAverageMetric(E which, String label) {
        metrics[which.ordinal()].setAverageLabel(label);
        return this;
    }

    /**
     * add an average metric for a counter
     *
     * @param which the counter
     */
    public Meter<E> addAverageMetric(E which) {
        return addAverageMetric(which, which.toString());
    }

    /**
     * add a rate metric for a counter
     *
     * @param which the counter
     * @param label label to use for reporting
     */
    public Meter<E> addRateMetric(E which, String label) {
        metrics[which.ordinal()].setRateLabel(label);
        return this;
    }

    /**
     * add a rate metric for a counter
     *
     * @param which the counter
     */
    public Meter<E> addRateMetric(E which) {
        return addRateMetric(which, which.toString());
    }

    /**
     * increment a counter (by one)
     */
    public void inc(E which) {
        counters[which.ordinal()].add(1);
    }

    /**
     * add val to a counter
     */
    public void inc(E which, long val) {
        counters[which.ordinal()].add(val);
    }

    /**
     * return the sum of all puts to a metric since last mark()
     */
    public long getCount(E which) {
        return counters[which.ordinal()].get().getCount();
    }

    /**
     * return the average of all puts to a metric since last mark()
     */
    public long getAverage(E which) {
        return counters[which.ordinal()].get().getAverage();
    }

    /**
     * return a metric's count divided by the time elapsed (in seconds) since
     * last mark()
     */
    public double getRate(E which) {
        return counters[which.ordinal()].get().getRate();
    }

    /**
     * reset a counter
     */
    public void reset(E which) {
        counters[which.ordinal()].mark();
    }

    /**
     * return the values of metrics since last mark()
     */
    public Map<String, Long> peek() {
        // get all the metrics - must do this once so state is consistent
        CounterState[] states = new CounterState[counters.length];
        for (int i = 0; i < counters.length; i++) {
            states[i] = counters[i].get();
        }
        return buildViews(states);
    }

    /**
     * return reset and get the values of metrics since last mark()
     */
    public Map<String, Long> mark() {
        // mark all the metrics
        CounterState[] states = new CounterState[counters.length];
        for (int i = 0; i < counters.length; i++) {
            states[i] = counters[i].mark();
        }
        return buildViews(states);
    }

    private Map<String, Long> buildViews(CounterState[] states) {
        // fetch state for each label and build output
        Map<String, Long> result = new LinkedHashMap<>();
        for (int i = 0; i < metrics.length; i++) {
            if (metrics[i] == null) {
                continue;
            }
            if (!LessStrings.isEmpty(metrics[i].getCountLabel())) {
                result.put(metrics[i].getCountLabel(), states[i].getCount());
            }
            if (!LessStrings.isEmpty(metrics[i].getMaxLabel())) {
                result.put(metrics[i].getMaxLabel(), states[i].getMax());
            }
            if (!LessStrings.isEmpty(metrics[i].getAverageLabel())) {
                result.put(metrics[i].getAverageLabel(), states[i].getAverage());
            }
            if (!LessStrings.isEmpty(metrics[i].getRateLabel())) {
                result.put(metrics[i].getRateLabel(), (long) states[i].getRate());
            }
        }
        return result;
    }

    /**
     * A helper class for counting events and computing event totals, average
     * event size and event rate.
     * this class uses native atomic operations instead of synchronized methods
     * to improve performance
     * for heavily threaded applications - results may be slightly skewed if a
     * put() spans a mark()
     */
    private static class Counter {
        private AtomicLong count;
        private AtomicLong max;
        private AtomicLong puts;
        private AtomicLong timestamp;

        public Counter() {
            count = new AtomicLong();
            max = new AtomicLong();
            puts = new AtomicLong();
            timestamp = new AtomicLong(System.currentTimeMillis());
        }

        /**
         * add val to the internal counter
         */
        public void add(long val) {
            count.addAndGet(val);
            if (val > max.get()) {
                max.set(val); // can miss concurrent writes greater than max
            }
            puts.incrementAndGet();
        }

        /**
         * return the counter states
         */
        public CounterState get() {
            return new CounterState(count.get(), max.get(), puts.get(), System.currentTimeMillis() - timestamp.get());
        }

        /**
         * return the counter states and reset their values
         */
        public CounterState mark() {
            long now = System.currentTimeMillis();
            return new CounterState(count.getAndSet(0), max.getAndSet(0), puts.getAndSet(0), now
                    - timestamp.getAndSet(now));
        }
    }

    private static class CounterState {
        private final long count;
        private final long max;
        private final long puts;
        private final long intervalms;

        public CounterState(long count, long max, long puts, long intervalms) {
            this.count = count;
            this.max = max;
            this.puts = puts;
            this.intervalms = intervalms;
        }

        /**
         * return the sum of all puts since last reset
         */
        public long getCount() {
            return count;
        }

        /**
         * return the largest put since last reset
         */
        public long getMax() {
            return max;
        }

        /**
         * return the average of all puts since last reset
         */
        public long getAverage() {
            return puts == 0 ? 0 : count / puts;
        }

        /**
         * return the sum of all puts divided by the time elapsed (in seconds)
         * since last reset
         */
        public double getRate() {
            return intervalms == 0 ? 0. : ((double) count * 1000) / intervalms;
        }
    }

    private static class Metric {
        private String countLabel;
        private String maxLabel;
        private String averageLabel;
        private String rateLabel;

        public Metric() {
        }

        public Metric(String countLabel, String maxLabel, String averageLabel, String rateLabel) {
            this.countLabel = countLabel;
            this.maxLabel = maxLabel;
            this.averageLabel = averageLabel;
            this.rateLabel = rateLabel;
        }

        public String getCountLabel() {
            return countLabel;
        }

        public String getMaxLabel() {
            return maxLabel;
        }

        public String getAverageLabel() {
            return averageLabel;
        }

        public String getRateLabel() {
            return rateLabel;
        }

        public void setCountLabel(String countLabel) {
            this.countLabel = countLabel;
        }

        public void setMaxLabel(String maxLabel) {
            this.maxLabel = maxLabel;
        }

        public void setAverageLabel(String averageLabel) {
            this.averageLabel = averageLabel;
        }

        public void setRateLabel(String rateLabel) {
            this.rateLabel = rateLabel;
        }
    }

}
