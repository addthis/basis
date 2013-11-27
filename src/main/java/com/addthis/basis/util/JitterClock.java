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
 * establishes a background thread updating a clock at a
 * 'jitter' interval.  for use by applications that would
 * like to frequently call System.getCurrentTimeMillis()
 * but aren't super sensitive to the returned time value.
 * this saves the overhead of constant native calls.
 */
public final class JitterClock extends Thread {
    private static JitterClock singleton = new JitterClock(10);

    public static long globalTime() {
        return time;
    }

    private static volatile long time;
    private final long jitter;

    private JitterClock(long jitter) {
        super("JitterClock jitter=" + jitter);
        this.jitter = jitter;
        setDaemon(true);
        update();
        start();
    }

    private void update() {
        time = System.currentTimeMillis();
    }

    public void run() {
        while (true) {
            try {
                sleep(jitter);
            } catch (Exception ex) {
                // ignore exceptions
            }
            update();
        }
    }
}
