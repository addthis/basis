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


import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Simple benchmark that mostly serves as an example. It measures the throughput of several possible
 * methods of bit reversal. The default settings provided in the annotations are super-fast/ rough,
 * and it is also worth noting that the added memory requirements of the Bytes.reverseBits() method
 * are not accounted for -- the CPU cache misses and flushes produced by it are outside of the scope
 * of these benchmarks. That said, depending on your JVM, hardware, etc, you may discover that a few
 * of the other methods that do not impose ambiguous memory costs also perform better. This is an
 * operation that already performs insanely fast, but it is a good lesson in unexpected performance
 * properties.
 *
 * To run this benchmark, do 'mvn clean package' from the bench directory, and then run
 * 'java -jar target/microbenchmarks.jar ".*BitReversals.*"'
 */
@BenchmarkMode(Mode.Throughput) // measure as ops/ time_unit
@OutputTimeUnit(TimeUnit.MICROSECONDS) // time_unit is microseconds
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS) // how long to warm up the jvm
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS) // how many runs to average over
@Fork(1) // how many JVM forks per test; measurements are run per fork
@Threads(1) // how many threads to run concurrently; thread count is per test -- not shared
@State(Scope.Thread) // treat this enclosing class as a State object that can be used in tests
public class BitReversals {

    /** our only actual state is this int we will increment to provide a bit of input variety */
    int inty = 0;

    /**
     * Each method marked with @Benchmark becomes a separate benchmark with its own warm ups,
     * measurements, forks, threads, and states. The static methods could be inlined -- the
     * extra method call in the first two would likely be inlined by the jit near instantly
     * anyway. However, this was easier to test and organize at the time.
     */

    @Benchmark
    public int reverseJdk() {
        inty += 1;
        return Integer.reverse(inty);
    }

    @Benchmark
    public int reverseBasis() {
        inty += 1;
        return Bytes.reverseBits(inty);
    }

    @Benchmark
    public int stackReverse() {
        inty += 1;
        return stackOverflowMethod(inty);
    }

    @Benchmark
    public int stanfordParallelReverse() {
        inty += 1;
        return stanfordParallelMethod(inty);
    }

    @Benchmark
    public int stanfordSevenOpReverse() {
        inty += 1;
        return stanfordSevenOpMethod(inty);
    }

    @Benchmark
    public int stanfordMultiplyReverse() {
        inty += 1;
        return stanfordMultiplyMethod(inty);
    }

    @Benchmark
    public int loopReverse() {
        inty += 1;
        return loopMethod(inty);
    }

    public static int loopMethod(int v1) {
        int s = 32;
        int mask = ~0;
        while ((s >>= 1) > 0) {
            mask ^= (mask << s);
            v1 = ((v1 >> s) & mask) | ((v1 << s) & ~mask);
        }
        return v1;
    }

    public static int stanfordSevenOpMethod(int v1) {
        int out = (((
                            (((v1 & 0xFF) * 0x0802) & 0x22110) |
                            (((v1 & 0xFF) * 0x08020) & 0x88440))
                    * 0x10101) & 0xFF0000) << 8;

        out |= ((
                        ((((v1 >>> 8) & 0xFF) * 0x0802) & 0x22110) |
                        ((((v1 >>> 8) & 0xFF) * 0x08020) & 0x88440))
                * 0x10101) & 0xFF0000;

        out |= (((
                         ((((v1 >>> 16) & 0xFF) * 0x0802) & 0x22110) |
                         ((((v1 >>> 16) & 0xFF) * 0x08020) & 0x88440))
                 * 0x10101) & 0xFF0000) >>> 8;

        out |= (((
                         ((((v1 >>> 24) & 0xFF) * 0x0802) & 0x22110) |
                         ((((v1 >>> 24) & 0xFF) * 0x0802) & 0x88440))
                 * 0x10101) & 0xFF0000) >>> 16;
        return out;
    }

    public static int stanfordMultiplyMethod(long v1) {
        return
                (int) (((((((v1 & 0xFF) * 0x80200802L) & 0x0884422110L) * 0x0101010101L) &
                         0xFF00000000L) >>> 8) |
                       (((((((v1 >>> 8) & 0xFF) * 0x80200802L) & 0x0884422110L) * 0x0101010101L) &
                         0xFF00000000L) >>> 16) |
                       (((((((v1 >>> 16) & 0xFF) * 0x80200802L) & 0x0884422110L) * 0x0101010101L) &
                         0xFF00000000L) >>> 24) |
                       (((((((v1 >>> 24) & 0xFF) * 0x80200802L) & 0x0884422110L) * 0x0101010101L) &
                         0xFF00000000L) >>> 32));
    }

    public static int stanfordParallelMethod(int v) {
        // swap odd and even bits
        v = ((v >>> 1) & 0x55555555) | ((v & 0x55555555) << 1);
        // swap consecutive pairs
        v = ((v >>> 2) & 0x33333333) | ((v & 0x33333333) << 2);
        // swap nibbles ...
        v = ((v >>> 4) & 0x0F0F0F0F) | ((v & 0x0F0F0F0F) << 4);
        // swap bytes
        v = ((v >>> 8) & 0x00FF00FF) | ((v & 0x00FF00FF) << 8);
        // swap 2-byte long pairs
        v = (v >>> 16) | (v << 16);
        return v;
    }

    public static int stackOverflowMethod(int x) {
        x = (x & 0x55555555) << 1 | (x & 0xAAAAAAAA) >>> 1;
        x = (x & 0x33333333) << 2 | (x & 0xCCCCCCCC) >>> 2;
        x = (x & 0x0F0F0F0F) << 4 | (x & 0xF0F0F0F0) >>> 4;
        x = (x & 0x00FF00FF) << 8 | (x & 0xFF00FF00) >>> 8;
        x = (x & 0x0000FFFF) << 16 | (x & 0xFFFF0000) >>> 16;
        return x;
    }
}
