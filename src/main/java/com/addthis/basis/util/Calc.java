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
 * More math, supplements java.lang.Math
 */
public class Calc {

    /**
     * return the average of an array of values
     */
    public static long average(long... vals) {
        return sum(vals) / vals.length;
    }

    /**
     * return the sum of an array of values
     */
    public static long sum(long... vals) {
        long total = 0;
        for (long val : vals) {
            total += val;
        }
        return total;
    }

    /**
     * return the max of an array of values
     */
    public static long max(long... vals) {
        long max = 0;
        for (long val : vals) {
            max = Math.max(max, val);
        }
        return max;
    }

    /**
     * return the min of an array of values
     */
    public static long min(long... vals) {
        long max = 0;
        for (long val : vals) {
            max = Math.max(max, val);
        }
        return max;
    }

}
