package com.addthis.basis.chars;

import java.io.IOException;

import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.UnmappableCharacterException;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufProcessor;

public final class CharBufs {

    public static final Charset UTF8 = Charset.forName("UTF-8");

    public static CharBuf ascii(CharBuffer charBuffer) {
        if (charBuffer.hasArray()) {
            return ascii(charBuffer.array(), charBuffer.arrayOffset(), charBuffer.length());
        } else {
            // just process as a CharSequence
            return ascii(charBuffer);
        }
    }

    public static CharBuf ascii(CharSequence charSequence) {
        ByteBuf data = ByteBufAllocator.DEFAULT.buffer(charSequence.length());
        CharBuf charBuf = new AsciiBuf(data);
        try {
            charBuf.append(charSequence);
        } catch (IOException iox) {
            throw new RuntimeException(iox);
        }
        return charBuf;
    }

    public static CharBuf ascii(char[] values, int start, int length) {
        ByteBuf data = ByteBufAllocator.DEFAULT.buffer(length);
        for (int i = start; i < start + length; i++) {
            char c =  values[i];
            if (c >= '\u0080') {
                throw new RuntimeException(new UnmappableCharacterException(2));
            }
            data.writeByte((byte) c);
        }
        return new AsciiBuf(data, false);
    }

    public static CharBuf ascii(char[] values) {
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
