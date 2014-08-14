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
import org.openjdk.jmh.infra.Blackhole;

import io.netty.buffer.Unpooled;

/**
 * Benchmarks various CharBuf classes against each other and String. This setup is designed
 * to mimic the work flow of "(I/O), decode ascii, perform some operation, encode ascii, (I/O)".
 *
 * Since half the point of CharBufs is to reduce much of the encoding/ decoding costs, the
 * expectation is that we should see String perform relatively worse compared to simple iteration,
 * and that is generally what we see. The "string.size" parameter can be used to adjust the test
 * string -- experimentally, String performs relatively worse at lower lengths eg. 15.
 *
 * There is one factor working against some CharBufs -- the creation of a new instance discards
 * knowledge about the nature of the underlying String. ie. the utf versions do not know that
 * all the characters are in the ascii set.
 */
public class CharSeqIterAndEncode {

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
        byte[] bytes;
        T string;

        @Setup(Level.Trial)
        public void makeStrings() {
            char[] values = new char[STRING_SIZE];
            for (int i = 0; i < STRING_SIZE; i++) {
                values[i] = (char) ((i % 100) + 5);
            }
            String asString = new String(values);
            bytes = asString.getBytes(StandardCharsets.UTF_8);
        }

        @Benchmark
        public char iterateString(Blackhole blackHole) throws IOException {
            if (string == null) {
                string = makeString(bytes);
                index = 0;
            }
            char c = string.charAt(index);
            index++;
            if (index >= STRING_SIZE) {
                blackHole.consume(encodeString(string));
                string = null;
            }
            return c;
        }

        public abstract T makeString(byte[] bytes);
        public abstract byte[] encodeString(T string);
    }

    public static class JavaString extends AbstractCharSeqBench<String> {

        @Override
        public String makeString(byte[] bytes) {
            return new String(bytes, StandardCharsets.UTF_8);
        }

        @Override
        public byte[] encodeString(String string) {
            return string.getBytes(StandardCharsets.UTF_8);
        }
    }

    public static class ByteArrayAscii extends AbstractCharSeqBench<ByteArrayReadOnlyAsciiBuf> {

        @Override
        public ByteArrayReadOnlyAsciiBuf makeString(byte[] bytes) {
            return new ByteArrayReadOnlyAsciiBuf(bytes);
        }

        @Override
        public byte[] encodeString(ByteArrayReadOnlyAsciiBuf string) {
            return string.data;
        }
    }

    public static class ByteArrayUtf extends AbstractCharSeqBench<ByteArrayReadOnlyUtfBuf> {

        @Override
        public ByteArrayReadOnlyUtfBuf makeString(byte[] bytes) {
            return new ByteArrayReadOnlyUtfBuf(bytes);
        }

        @Override
        public byte[] encodeString(ByteArrayReadOnlyUtfBuf string) {
            return string.data;
        }
    }

    public static class ByteBufUtf extends AbstractCharSeqBench<ReadOnlyUtfBuf> {

        @Override
        public ReadOnlyUtfBuf makeString(byte[] bytes) {
            return new ReadOnlyUtfBuf(Unpooled.wrappedBuffer(bytes));
        }

        @Override
        public byte[] encodeString(ReadOnlyUtfBuf string) {
            return string.content().array();
        }
    }
}
