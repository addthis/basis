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

import java.lang.CharSequence;import java.lang.Integer;import java.lang.String;import java.nio.charset.StandardCharsets;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@State(Scope.Thread)
public class CharSequenceIterations {

    public static final int STRING_SIZE = Integer.getInteger("string.size", 64000);

    int index = 0;

    @Setup(Level.Trial)
    public void makeStrings() {
        char[] values = new char[STRING_SIZE];
        for (int i = 0; i < STRING_SIZE; i++) {
            values[i] = (char) ((i % 100) + 5);
        }
        String asString = new String(values);
        byte[] bytes = asString.getBytes(StandardCharsets.UTF_8);
        string = asString;
//        ascii = new ByteArrayReadOnlyAsciiBuf(bytes);
//        utf = new ByteArrayReadOnlyUtfBuf(bytes);
//        utfCharBuf = new ReadOnlyUtfBuf(Unpooled.wrappedBuffer(bytes));
    }

    public CharSequence string;

    @Benchmark
    public char iterateString() throws IOException {
        char c = string.charAt(index);
        index++;
        if (index >= STRING_SIZE) {
            index = 0;
        }
        return c;
    }

    public CharSequence ascii;

    @Benchmark
    public char iterateAscii() throws IOException {
        char c = ascii.charAt(index);
        index++;
        if (index >= STRING_SIZE) {
            index = 0;
        }
        return c;
    }

    public CharSequence utf;

    @Benchmark
    public char iterateUtf() throws IOException {
        char c = utf.charAt(index);
        index++;
        if (index >= STRING_SIZE) {
            index = 0;
        }
        return c;
    }

    public CharSequence utfCharBuf;

    @Benchmark
    public char iterateUtfCharBuf() throws IOException {
        char c = utfCharBuf.charAt(index);
        index++;
        if (index >= STRING_SIZE) {
            index = 0;
        }
        return c;
    }
}
