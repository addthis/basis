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

import java.nio.ByteBuffer;

/**
 * This is a utility class for encoding numbers and bytes into the 90 characters that
 * are absolutely guaranteed to work in cookies across all broswers for a long time.
 *
 * There are a few main ways to use this class:
 * {@link #encodeBase45(long, boolean, boolean)} and {@link #decodeBase45(CharSequence)}
 * provide a way to encode positive numbers only into a cookie string in a way that can be used without delimiters.
 *
 * {@link #encodeBase90Signed(long)}, {@link #encodeBase90Unsigned(long)}
 * and {@link #decodeBase90(CharSequence, boolean)} provide a way to encode numbers into a
 * cookie string in the most efficient encoding. The unsigned version returns a variable length
 * string, while the signed version will always return a string of length 10.
 *
 * A better use of <code>encodeBase90Signed</code> is encoding binary data. That's what
 * {@link #encodeBytesBase90(byte[])} and {@link #decodeBytesBase90(CharSequence)} are for.
 * These methods allow encoding and decoding of arbitrary data. The byte array is converted
 * to longs, encoded using <code>encodeBase90Signed</code>, and there is one extra character
 * at the end telling the decoder how many bytes the last decoded long represents.
 *
 * @see <a href="http://stackoverflow.com/a/1969339/1238727">Allowed characters in cookies</a>
 */
public class CookieSafeBase90 {
    // these are the 90 chars we can use for the encoding. they are ordered in numerical order.
    private static final char[] BASECHARS = ("!#$%&'()*+-./0123456789:<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[]^_" +
                                             "`abcdefghijklmnopqrstuvwxyz{|}~").toCharArray();
    private static final int BASE45 = 45;
    private static final int BASE90 = 90;
    // our char arrays need to be big enough to handle any long in base45.
    private static final int MAX_NUMBER_LENGTH = 12;
    /**
     * Exposed for use by code using {@link #encodeBase45(long, boolean, boolean)}
     * Cookie parsers can use this to determine whether a character is in the high or low range of base90 characters.
     * For example, if one type of number was encoded with flipLastByte=true, the parser
     * would need to check if each character was high or low to find the end of the encoded number.
     */
    public static final int MIDDLE_CHAR = BASECHARS[BASE45];

    /**
     * Encodes a number into a string that can be used without delimiters.
     * Only positive numbers can be encoded. This method can be used to make custom cookie formats.
     *
     * @param value         the number to encode
     * @param highRange     whether to use the bottom 45 or top 45 characters
     * @param flipLastByte  if true, the last character will be from the other range.
     *                      This also means that all numbers get encoded with at least
     *                      two characters; the first character will be a "zero" character.
     * @return              The encoded string
     */
    public static String encodeBase45(long value, boolean highRange, boolean flipLastByte) {
        char[] out = new char[MAX_NUMBER_LENGTH];
        for (int i = out.length - 1;; i--) {
            boolean isFlip = flipLastByte && (i == (out.length - 1));
            int index = (int) Math.abs(value % BASE45);
            // NOTE: this is non-obvious how this if statement works!
            // if high and no flip, or if low and flip, add base
            // if high and flip, or if low and no flip, do nothing
            if (isFlip != highRange) {
                index += BASE45;
            }
            value = value / BASE45;
            out[i] = BASECHARS[index];
            int currentSize = out.length - i;
            boolean isLast = (value == 0) || (i == 0);
            boolean isDone = isLast && ((currentSize != 1) || !flipLastByte);
            // return string, unless size 0 and we need to flip the last byte (needs 0-pad left)
            if (isDone) {
                return new String(out, i, currentSize);
            }
        }
    }

    /**
     * Encodes a positive number into base 90, returning a variable string length.
     * Negative numbers will be encoded as if they were positive.
     *
     * @param value the number to encode
     * @return      the encoded string
     */
    public static String encodeBase90Unsigned(long value) {
        return encodeBase90(value, 0, false);
    }

    /**
     * Encodes a positive or negative number into base 90. The returned string will always be
     * ten characters. The fixed length encoding makes adding sign information easy.
     * This is used by {@link #encodeBytesBase90(byte[])} but is public in case it is useful.
     *
     * @param value the number to encode
     * @return      the encoded string
     */
    public static String encodeBase90Signed(long value) {
        return encodeBase90(value, 10, true);
    }

    // this could be made public if someone really needs it someday
    // but the for now the more complex signature is hidden.
    private static String encodeBase90(long value, int minLength, boolean signed) {
        boolean negative = signed && (value < 0);
        char[] out = new char[MAX_NUMBER_LENGTH];
        for (int i = out.length - 1; ; i--) {
            int index = (int) Math.abs(value % BASE90);
            // represent signed numbers
            if ((i == 0) && (value < 0)) {
                index += BASE45;
            }
            out[i] = BASECHARS[index];
            value = value / BASE90;
            if ((--minLength <= 0) && ((value == 0) || (i == 0))) {
                // encode negative numbers
                if (negative) {
                    out[i] = BASECHARS[index + BASE45];
                }
                return new String(out, i, out.length - i);
            }
        }
    }

    /**
     * Encodes arbitrary bytes to a cookie safe string in a very efficent encoding.
     * Each sequence of up to 8 bytes is converted into a long and encoded into a
     * ten-character string using {@link #encodeBase90Signed(long)}. Then a final
     * character is added representing how many bytes in the last decoded long are
     * actually encoded bytes, for cases where <code>bytes</code> is not divisible by 8.
     *
     * @param bytes the data to encode
     * @return      the encoded string
     */
    public static String encodeBytesBase90(byte[] bytes) {
        // each 8 bytes = 10 chars
        int len = bytes.length;
        StringBuilder builder = new StringBuilder(((((len + Long.BYTES) - 1) / Long.BYTES) * 10) + 1);
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        int lastWordBytesToKeep = 8;
        while(buffer.remaining() > 0) {
            int remaining = buffer.remaining();
            final long valueToEncode;
            if (remaining >= Long.BYTES) {
                valueToEncode = buffer.getLong();
            } else {
                buffer.position(len);
                ByteBuffer temp = ByteBuffer.allocate(Long.BYTES + remaining);
                temp.put(bytes, len - remaining, remaining);
                temp.putLong(0L);
                temp.rewind();
                valueToEncode = temp.getLong();
                lastWordBytesToKeep = remaining;
            }
            String encoded = encodeBase90Signed(valueToEncode);
            builder.append(encoded);
        }
        builder.append(encodeBase90Unsigned((long) lastWordBytesToKeep));
        return builder.toString();
    }

    /**
     * Decodes arbitrary bytes that were encoded by {@link #encodeBytesBase90(byte[])}.
     *
     * @param charSequence  the cookie string to decode
     * @return              a decoded array of bytes
     */
    public static byte[] decodeBytesBase90(CharSequence charSequence) {
        int len = charSequence.length() - 1;
        int lastWordBytesToKeep = (int) decodeBase90(charSequence.subSequence(len, len + 1), true);
        ByteBuffer buffer = ByteBuffer.allocate((((len / 10) - 1) * Long.BYTES) + lastWordBytesToKeep);
        for (int pos = 0; pos < len; pos += 10) {
            long decoded = decodeBase90(charSequence.subSequence(pos, pos + 10), true);
            if ((pos + 10) == len) {
                ByteBuffer temp = ByteBuffer.allocate(Long.BYTES);
                temp.putLong(decoded);
                for (int i = 0; i < lastWordBytesToKeep; i++) {
                    buffer.put(temp.get(i));
                }
            } else {
                buffer.putLong(decoded);
            }
        }
        return buffer.array();
    }

    /**
     * All characters are in numerical order in the BASECHARS array. Here we
     * simply find the position of the char in that array and return the index.
     *
     * @param  c    the char to convert from base 90
     * @return      the value of the char in base 10
     */
    private static int decodeChar(int c) {
        int decoded = 0;
        if ('!' == c) {
            decoded = 0;
        } else if ((c >= '#') && (c <= '+')) {
            decoded = (c - '#') + 1;
        } else if ((c >= '-') && (c <= ':')) {
            decoded = (c - '-') + 10;
        } else if ((c >= '<') && (c <= '[')) {
            decoded = (c - '<') + 24;
        } else if ((c >= ']') && (c <= '~')) {
            decoded = (c - ']') + 56;
        } else {
            throw new RuntimeException("invalid base encoding: " + c);
        }
        return decoded;
    }

    /**
     * Decodes a long from a cookie string encoded by {@link #encodeBase90Signed(long)}
     * or {@link #encodeBase90Unsigned(long)}. This method cannot determine which method
     * was used to encode, it must be told if the number is signed.
     *
     * @param charSequence  The cookie string to decode
     * @param signed        true if the number was encoded with <code>encodeBase90Signed</code>
     * @return              the decoded number
     */
    public static long decodeBase90(CharSequence charSequence, boolean signed) {
        long sum = 0;
        // switches to -1 if negative
        long sign = 1;
        int len = charSequence.length();
        for (int i = 0; i < len; i++) {
            final int c = (int) charSequence.charAt(i);
            int add = decodeChar(c);
            // handle negative number decoding
            if (signed && (i == 0) && (c >= MIDDLE_CHAR)) {
                add -= BASE45;
                sign = -1;
            }
            sum = (sum * (long) BASE90) + (long) add;
        }
        return sum * sign;
    }

    /**
     * Decodes a long from a cookie string that was encoded by {@link #encodeBase45(long, boolean, boolean)}.
     * Does not handle negative numbers. Does handle all 4 possible permutations of each number.
     *
     * @param charSequence  The cookie string to decode
     * @return              the decoded number
     */
    public static long decodeBase45(CharSequence charSequence) {
        long sum = 0;
        for (int i = 0; i < charSequence.length(); i++) {
            final int c = (int) charSequence.charAt(i);
            int add = decodeChar(c);
            // convert base90 to base45 if necessary
            if (add >= BASE45) {
                add -= BASE45;
            }
            sum = (sum * (long) BASE45) + (long) add;
        }
        return sum;
    }
}
