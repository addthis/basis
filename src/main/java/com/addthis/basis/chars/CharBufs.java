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

import java.nio.charset.UnmappableCharacterException;

import com.google.common.annotations.Beta;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.buffer.ByteBufProcessor;

/**
 * Static utility methods; likely subject to heavy change.
 */
@Beta
public final class CharBufs {
    private CharBufs() {}

    public static ReadableCharBuf utf(byte[] bytes) {
        return new ByteArrayReadOnlyUtfBuf(bytes);
    }

    public static ReadableCharBuf utf(ByteBuf bytes) {
        return new ReadOnlyUtfBuf(bytes);
    }

    public static ReadableCharBuf utf(ByteBufHolder byteHolder) {
        return new ReadOnlyUtfBuf(byteHolder.content());
    }

    public static ReadableCharBuf ascii(char[] values, int start, int length) {
        byte[] data = new byte[length];
        for (int i = start; i < (start + length); i++) {
            char c =  values[i];
            if (c >= '\u0080') {
                throw new RuntimeException(new UnmappableCharacterException(2));
            }
            data[i] = (byte) c;
        }
        return new ByteArrayReadOnlyAsciiBuf(data);
    }

    public static ReadableCharBuf ascii(char[] values) {
        return ascii(values, 0, values.length);
    }

    /** Aborts on negatives aka. non-ascii characters. */
    public static final ByteBufProcessor FIND_NEGATIVE = new ByteBufProcessor() {
        @Override
        public boolean process(byte value) throws Exception {
            return value < 0;
        }
    };
}
