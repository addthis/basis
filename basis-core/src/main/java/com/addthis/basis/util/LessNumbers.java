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

import java.util.Random;
import java.util.UUID;

import java.math.BigInteger;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashFunction;

public class LessNumbers {
    private static final HashFunction fallbackHashFunc = Hashing.murmur3_128();
    private static final long SIX_BIT_MASK = 63;


    /**
     * takes a number in a human readable format (5BM, 6GB, 102K) and returns a long.
     *
     * @param num a human readable format (5BM, 6GB, 102K)
     * @return a long number equivalent to the string representation
     */
    public static long parseHumanReadable(String num) {
        num = num.toUpperCase();
        if (num.endsWith("M") || num.endsWith("MB")) {
            return Long.parseLong(num.substring(0, num.indexOf("M"))) * 1024 * 1024;
        } else if (num.endsWith("MIB")) {
            return Long.parseLong(num.substring(0, num.indexOf("M"))) * 1000 * 1000;
        } else if (num.endsWith("K") || num.endsWith("KB")) {
            return Long.parseLong(num.substring(0, num.indexOf("K"))) * 1024;
        } else if (num.endsWith("KIB")) {
            return Long.parseLong(num.substring(0, num.indexOf("K"))) * 1000;
        } else if (num.endsWith("G") || num.endsWith("GB")) {
            return Long.parseLong(num.substring(0, num.indexOf("G"))) * 1024 * 1024 * 1024;
        } else if (num.endsWith("GIB")) {
            return Long.parseLong(num.substring(0, num.indexOf("G"))) * 1000 * 1000 * 1000;
        } else {
            return Long.parseLong(num);
        }
    }

    /**
     * integer
     */
    public static int parseInt(String val, int def, int radix) {
        try {
            return Integer.parseInt(val, radix);
        } catch (Exception ex) {
            return def;
        }
    }

    /**
     * long
     */
    public static long parseLong(String val, long def, int radix) {
        try {
            return Long.parseLong(val, radix);
        } catch (Exception ex) {
            return def;
        }
    }

    /**
     * float
     */
    public static float parseFloat(String val, float def) {
        try {
            return Float.parseFloat(val);
        } catch (Exception ex) {
            return def;
        }
    }

    /**
     * double
     */
    public static double parseDouble(String val, double def) {
        try {
            return Double.parseDouble(val);
        } catch (Exception ex) {
            return def;
        }
    }

    /**
     * Converts a base-encoded string to a long.
     * Does not handle negative numbers;
     */
    public static long longFromBase(String val, int base) {
        return longFromBase(val, base, false);
    }

    public static long longFromBase(String val, int base, boolean fallback) {
        if (base > basechars.length) {
            if (fallback) {
                return fallbackHashFunc.hashUnencodedChars(val).asLong();
            }
            throw new RuntimeException(base + " outside base range of 2-" + basechars.length);
        }
        long rv = 0;
        int length = val.length();
        for (int i = 0; i < length; i++) {
            char aCv = val.charAt(i);
            rv *= base;
            int av;
            if (aCv >= '0' && aCv <= '9') {
                av = aCv - '0';
            } else if (aCv >= 'a' && aCv <= 'z') {
                av = aCv - 'a' + 10;
            } else if (aCv >= 'A' && aCv <= 'Z') {
                av = aCv - 'A' + 36;
            } else {
                if (fallback) {
                    return fallbackHashFunc.hashUnencodedChars(val).asLong();
                }
                throw new RuntimeException("invalid base encoding: " + val);
            }
            if (av >= base) {
                if (fallback) {
                    return fallbackHashFunc.hashUnencodedChars(val).asLong();
                }
                throw new RuntimeException("chars outside of base range: " + val);
            }
            rv += av;
        }
        return rv;
    }

    /**
     * Converts a base-encoded string to an integer.
     * Does not handle negative numbers;
     */
    public static int intFromBase(String val, int base) {
        if (base > basechars.length) {
            throw new RuntimeException(base + " outside base range of 2-" + basechars.length);
        }
        int rv = 0;
        int length = val.length();
        for (int i = 0; i < length; i++) {
            char aCv = val.charAt(i);
            rv *= base;
            rv += charToDigit(aCv, val);
        }
        return rv;
    }

    /**
     * Array of chars for generating numbers in alternate bases.
     */
    static final char[] basechars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-_".toCharArray();
    static final BigInteger[] bigIntegerChars = new BigInteger[basechars.length];
    static {
        int offset = 0;
        for(char i = '0'; i <= '9'; i++) {
            bigIntegerChars[offset++] = BigInteger.valueOf(i - '0');
        }
        for(char i = 'a'; i <= 'z'; i++) {
            bigIntegerChars[offset++] = BigInteger.valueOf(i - 'a' + 10);
        }
        for(char i = 'A'; i <= 'Z'; i++) {
            bigIntegerChars[offset++] = BigInteger.valueOf(i - 'A' + 36);
        }
        bigIntegerChars[offset++] = BigInteger.valueOf(62);
        bigIntegerChars[offset++] = BigInteger.valueOf(63);
    }

    public static final int MAX_BASE = basechars.length;
    /**
     * String containing zeros for pre-padding base-36 longs.
     */
    private static final String[] pads = new String[MAX_BASE];
    /**
     * Public pre-generated Random for what ailes you.
     */
    public static final Random random = new Random(System.currentTimeMillis());

    /**
     * Converts a long to a base-encoded string.  Does not handle negative numbers;
     */
    public static String toBase(long val, int base) {
        return toBase(val, base, -1);
    }

    public static String toBase(long val, int base, int minlen) {
        if (base > basechars.length) {
            throw new RuntimeException(base + " outside base range of 2-" + basechars.length);
        }
        char[] out = new char[128];
        for (int i = out.length - 1;; i--) {
            out[i] = basechars[(int) Math.abs(val % base)];
            val = val / base;
            if (--minlen <= 0 && (val == 0 || i == 0)) {
                return new String(out, i, out.length - i);
            }
        }
    }

    /**
     * Generates a random long in specified pre-padded with 0s.
     *
     * @return long in specified base pre-padded with 0s
     */
    static String nextLong(int base) {
        synchronized (pads) {
            String pad = pads[base - 1];
            if (pad == null) {
                pad = LessBytes.clear(toBase(Long.MAX_VALUE, base), '0');
                pads[base - 1] = pad;
            }
            String nv = toBase(random.nextLong(), base);
            return pad.substring(nv.length()) + nv;
        }
    }

    /**
     * Array of bytes for generating hex strings.
     */
    public static final byte[] hex = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    public static final byte[] HEX = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    private static final boolean verboseHex = System.getProperty("xhex", "0").equals("1");

    public static String toHexString(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (byte c : b) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            char c1 = (char) hex[(c >> 4) & 0xf];
            char c2 = (char) hex[(c & 0xf)];
            sb.append(c1);
            sb.append(c2);
            if (verboseHex) {
                sb.append(' ');
                if (c >= 32 && c <= 126) {
                    sb.append((char) c);
                } else {
                    sb.append('.');
                }
            }
        }
        return sb.toString();
    }

    /**
     * Parse integer with given radix and default value on errors
     */
    public static int parseInt(int radix, String val, int def) {
        try {
            return Integer.parseInt(val, radix);
        } catch (Exception ex) {
            return def;
        }
    }

    /**
     * Parse integer with given radix and default value on errors
     */
    public static long parseLong(int radix, String val, long def) {
        try {
            return Long.parseLong(val, radix);
        } catch (Exception ex) {
            return def;
        }
    }

    public static long longFromBase36(String val) {
        return longFromBase(val.toLowerCase(), 36);
    }

    public static long longFromBase64(String val) {
        return longFromBase(val, 64);
    }

    public static String toBase36(long val) {
        return toBase(val, 36).toUpperCase();
    }

    public static int intFromBase36(String val) {
        return intFromBase(val.toLowerCase(), 36);
    }

    public static String toBase64(long val) {
        return toBase(val, 64);
    }

    public static int intFromBase64(String val) {
        return intFromBase(val, 64);
    }

    static int charToDigit(char character, String val) {
        if (character >= '0' && character <= '9') {
            return character - '0';
        } else if (character >= 'a' && character <= 'z') {
            return character - 'a' + 10;
        } else if (character >= 'A' && character <= 'Z') {
            return character - 'A' + 36;
        } else if (character == '-') {
            return 62;
        } else if (character == '_') {
            return 63;
        } else {
            throw new RuntimeException("invalid base encoding: " + val);
        }
    }

    public static BigInteger bigIntegerFromBase(String val, int base) {
        if (base > MAX_BASE) {
                throw new RuntimeException(base + " outside base range of 2-" + MAX_BASE);
        }
        BigInteger rv = BigInteger.ZERO;
        BigInteger biBase = BigInteger.valueOf(base);
        int length = val.length();
        for (int i = 0; i < length; i++) {
            rv = rv.multiply(biBase);
            char cvv = val.charAt(i);
            if (cvv >= '0' && cvv <= '9') {
                rv = rv.add(bigIntegerChars[cvv - '0']);
            } else if (cvv >= 'a' && cvv <= 'z') {
                rv = rv.add(bigIntegerChars[cvv - 'a' + 10]);
            } else if (cvv >= 'A' && cvv <= 'Z') {
                rv = rv.add(bigIntegerChars[cvv - 'A' + 36]);
            } else if (cvv == '-') {
                rv = rv.add(bigIntegerChars[62]);
            } else if (cvv == '_') {
                rv = rv.add(bigIntegerChars[63]);
            } else {
                throw new RuntimeException("invalid base encoding: " + val);
            }
        }
        return rv;
    }

    public static UUID UUIDFromBase64(String val) {
        long hiBits = 0;
        int pos = 0;
        for(int i = 0; i < 11; i++) {
            hiBits <<= 6;
            hiBits += charToDigit(val.charAt(pos++), val);
        }
        int mixBits = charToDigit(val.charAt(pos++), val);
        hiBits <<= 2;
        hiBits += (mixBits >>> 4);
        /**
         * loBits only needs to low 4 bits out of the 6 bits
         * in mixBits but the high 2 bits are going to
         * fall off anyway as a result of the left shift operations.
         */
        long loBits = mixBits;
        for(int i = 0; i < 10; i++) {
            loBits <<= 6;
            loBits += charToDigit(val.charAt(pos++), val);
        }
        return new UUID(hiBits, loBits);
    }

    public static String toBase(BigInteger val, int base) {
        return toBase(val, base, -1);
    }

    public static String toBase(BigInteger val, int base, int minlen) {
        if (base > MAX_BASE) {
                throw new RuntimeException(base + " outside base range of 2-" + MAX_BASE);
        }
        char[] out = new char[128];
        BigInteger biBase = BigInteger.valueOf(base);
        for (int i = out.length - 1;; i--) {
            BigInteger[] divAndRem = val.divideAndRemainder(biBase);
            out[i] = basechars[Math.abs(divAndRem[1].intValue())];
            val = divAndRem[0];
            if (--minlen <= 0 && (val.equals(BigInteger.ZERO) || i == 0)) {
                return new String(out, i, out.length - i);
            }
        }
    }

    public static String toBase64(UUID val) {
        long hiBits = val.getMostSignificantBits();
        long loBits = val.getLeastSignificantBits();
        char[] out = new char[22];
        int pos = 21;
        for(int i = 0; i < 10; i++) {
            out[pos--] = basechars[(int) (loBits & SIX_BIT_MASK)];
            loBits >>>= 6;
        }
        int mixBits = (((int) (hiBits & 3)) << 4) | ((int) loBits);
        out[pos--] = basechars[mixBits];
        hiBits >>>= 2;
        for(int i = 0; i < 11; i++) {
            out[pos--] = basechars[(int) (hiBits & SIX_BIT_MASK)];
            hiBits >>>= 6;
        }
        return new String(out);
    }

    public static String toBase36(BigInteger val) {
        return toBase(val, 36).toUpperCase();
    }

    public static BigInteger bigIntegerFromBase36(String val) {
        return bigIntegerFromBase(val.toLowerCase(), 36);
    }

    public static String toBase64(BigInteger val) {
        return toBase(val, 64);
    }

    public static BigInteger bigIntegerFromBase64(String val) {
        return bigIntegerFromBase(val, 64);
    }
}
