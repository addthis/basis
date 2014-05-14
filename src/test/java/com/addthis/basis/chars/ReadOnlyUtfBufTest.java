package com.addthis.basis.chars;

import java.nio.charset.StandardCharsets;

import io.netty.buffer.Unpooled;

public class ReadOnlyUtfBufTest extends CharSequenceTests {

    public ReadOnlyUtfBufTest(CharSequence controlSequence) {
        super(controlSequence);
    }

    @Override
    public CharSequence getCharSequenceForString(CharSequence control) {
        return new ReadOnlyUtfBuf(Unpooled.wrappedBuffer(control.toString().getBytes(StandardCharsets.UTF_8)));
    }
}
