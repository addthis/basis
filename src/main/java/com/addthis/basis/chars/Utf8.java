package com.addthis.basis.chars;

public class Utf8 {

    // 110xxx1x; anything more negative cannot be sequence start (eg. 10xxxxxx)
    public static final byte MIN_MULTI_HEADER = (byte) 0xC2;
    public static final byte MIN_THREE_HEADER = (byte) 0xE0;
    public static final byte MIN_FOUR_HEADER = (byte) 0xF0;

    // these masks only work AFTER the byte is known to be negative
    // they leave only the 1 bit that signals N bytes or greater. Suitable for & >> combos
    public static final byte TWO_BYTE_MASK = (byte) 0x40;
    public static final byte TWO_BYTE_SHIFT = (byte) 6;
    public static final byte THREE_BYTE_MASK = (byte) 0x20;
    public static final byte THREE_BYTE_SHIFT = (byte) 5;
    public static final byte FOUR_BYTE_MASK = (byte) 0x10;
    public static final byte FOUR_BYTE_SHIFT = (byte) 4;

    // xx111111; continuation bytes have 10 as a header and the first two bits should be ignored
    public static final byte CONTINUATION_MASK = (byte) 0x3F;

    public static final byte TWO_BYTE_HEADER_MASK = (byte) 0x1F;
    public static final byte THREE_BYTE_HEADER_MASK = (byte) 0x0F;
    public static final byte FOUR_BYTE_HEADER_MASK = (byte) 0x07;

}
