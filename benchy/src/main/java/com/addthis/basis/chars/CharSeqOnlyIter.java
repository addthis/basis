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

package com.addthis.basis.chars;

import java.io.IOException;

import java.util.concurrent.TimeUnit;

import java.nio.charset.StandardCharsets;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

import io.netty.buffer.Unpooled;

/**
 * Benchmarks various CharBuf classes against each other and String. This setup is designed
 * to test iteration over CharSequences via the charAt method for character strings that
 * (perhaps unknown to the implementation) consist entirely of ascii characters. Iteration
 * is a theoretical sore spot since although almost all string operations rely on it, the api
 * only supports it through 'random' lookups. Random lookups are in turn a sore spot for
 * string representations that have variable width characters.
 *
 * This case is likely easier for CharBufs than non-ascii purely random look-ups, but sequential
 * access to ascii characters is the use case we expect to be most common. Locally, I have seen
 * both ByteArray CharBufs out perform String (one knows it is ascii only, and the other does not).
 */
public class CharSeqOnlyIter {

    public static final int STRING_SIZE = Integer.getInteger("string.size", 64000);

    @BenchmarkMode(Mode.Throughput) // measure as ops/ time_unit
    @OutputTimeUnit(TimeUnit.MICROSECONDS) // time_unit is microseconds
    @Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS) // how long to warm up the jvm
    @Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS) // how many runs to average over
    @Fork(1) // how many JVM forks per test; measurements are run per fork
    @Threads(1) // how many threads to run concurrently; thread count is per test -- not shared
    @State(Scope.Thread)
    public abstract static class AbstractCharSeqBench<T extends CharSequence> {

        int index = 0;
        T string;

        @Setup(Level.Trial)
        public void makeStrings() {
            char[] values = new char[STRING_SIZE];
            for (int i = 0; i < STRING_SIZE; i++) {
                values[i] = (char) ((i % 100) + 5);
            }
            String asString = new String(values);
            byte[] bytes = asString.getBytes(StandardCharsets.UTF_8);
            string = makeString(bytes);
        }


        @Benchmark
        public char iterateString() throws IOException {
            char c = string.charAt(index);
            index++;
            if (index >= STRING_SIZE) {
                index = 0;
            }
            return c;
        }

        public abstract T makeString(byte[] bytes);
    }

    public static class JavaString extends AbstractCharSeqBench<String> {

        @Override
        public String makeString(byte[] bytes) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    public static class ByteArrayAscii extends AbstractCharSeqBench<ByteArrayReadOnlyAsciiBuf> {

        @Override
        public ByteArrayReadOnlyAsciiBuf makeString(byte[] bytes) {
            return new ByteArrayReadOnlyAsciiBuf(bytes);
        }
    }

    public static class ByteArrayUtf extends AbstractCharSeqBench<ByteArrayReadOnlyUtfBuf> {

        @Override
        public ByteArrayReadOnlyUtfBuf makeString(byte[] bytes) {
            return new ByteArrayReadOnlyUtfBuf(bytes);
        }
    }

    public static class ByteBufUtf extends AbstractCharSeqBench<ReadOnlyUtfBuf> {

        @Override
        public ReadOnlyUtfBuf makeString(byte[] bytes) {
            return new ReadOnlyUtfBuf(Unpooled.wrappedBuffer(bytes));
        }
    }
}
