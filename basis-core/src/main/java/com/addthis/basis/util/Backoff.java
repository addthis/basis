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

/**
 * Backoff interval manager, useful for throttling back things like polling and
 * reconnect timeouts. Backoff intervals start at min and will double if
 * requested in immediate succession (bounded by max). Intervals will settle
 * back down to min as time elapses since the last backoff. A typical use is to
 * call Backoff.sleep() in your error/miss handler.
 */
public class Backoff {
    private int min;
    private int max;
    private long lastBackoff;
    private long lastGet;

    /**
     * @param min starting backoff interval in milliseconds
     * @param max maximum backoff interval in milliseconds
     */
    public Backoff(int min, int max) {
        this.min = min;
        this.max = max;
        lastBackoff = min;
    }

    /**
     * get the next backoff interval, starting with the min value. subsequent
     * requests are computed as twice the last interval, minus the time that has
     * elapsed since the last interval ended (bounded by min and max).
     */
    public long get() {
        long now = System.currentTimeMillis();
        long backoff = lastBackoff * 3 - (now - lastGet);
        backoff = Math.max(min, Math.min(max, backoff));  // bound by min and max
        lastBackoff = backoff;
        lastGet = now;
        return backoff;
    }

    /**
     * compute the backoff interval and sleep for that period of time
     */
    public void sleep() throws InterruptedException {
        Thread.sleep(get());
    }
}
