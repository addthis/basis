package com.addthis.basis.chars;

import java.nio.charset.UnmappableCharacterException;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.buffer.ByteBufProcessor;

public final class CharBufs {

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

    /**
     * Aborts on negatives
     */
    public static final ByteBufProcessor FIND_NEGATIVE = new ByteBufProcessor() {
        @Override
        public boolean process(byte value) throws Exception {
            return value < 0;
        }
    };

    // unused for utility class
    private CharBufs() {
    }
}
