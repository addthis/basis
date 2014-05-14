package com.addthis.basis.chars;

public class ByteArrayReadOnlyUtfBufTest extends CharSequenceTests {

    public ByteArrayReadOnlyUtfBufTest(CharSequence controlSequence) {
        super(controlSequence);
    }

    @Override
    public CharSequence getCharSequenceForString(CharSequence control) {
        return new ByteArrayReadOnlyUtfBuf(control.toString());
    }
}
