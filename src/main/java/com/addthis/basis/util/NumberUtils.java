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

import java.math.BigInteger;

import java.util.Random;

public class NumberUtils {

    /**
     * Converts a base-encoded string to an integer. Does not handle negative
     * numbers;
     */
    public static int intFromBase(String val, int base) {
        if (base > basechars.length) {
            throw new RuntimeException(base + " outside base range of 2-" + basechars.length);
        }
        int rv = 0;
        char cv[] = val.toCharArray();
        for (char aCv : cv) {
            rv *= base;
            if (aCv >= '0' && aCv <= '9') {
                rv += aCv - '0';
            } else if (aCv >= 'a' && aCv <= 'z') {
                rv += aCv - 'a' + 10;
            } else if (aCv >= 'A' && aCv <= 'Z') {
                rv += aCv - 'A' + 36;
            } else if (aCv == '-') {
                rv += 62;
            } else if (aCv == '_') {
                rv += 63;
            } else {
                throw new RuntimeException("invalid base encoding: " + val);
            }
        }
        return rv;
    }

    /**
     * Converts a base-encoded string to an long. Does not handle negative
     * numbers;
     */
    public static long longFromBase(String val, int base) {
        if (base > basechars.length) {
            throw new RuntimeException(base + " outside base range of 2-" + basechars.length);
        }
        long rv = 0;
        char cv[] = val.toCharArray();
        for (char aCv : cv) {
            rv *= base;
            if (aCv >= '0' && aCv <= '9') {
                rv += aCv - '0';
            } else if (aCv >= 'a' && aCv <= 'z') {
                rv += aCv - 'a' + 10;
            } else if (aCv >= 'A' && aCv <= 'Z') {
                rv += aCv - 'A' + 36;
            } else if (aCv == '-') {
                rv += 62;
            } else if (aCv == '_') {
                rv += 63;
            } else {
                throw new RuntimeException("invalid base encoding: " + val);
            }
        }
        return rv;
    }

    public static BigInteger bigIntegerFromBase(String val, int base) {
        if (base > basechars.length) {
                throw new RuntimeException(base + " outside base range of 2-" + basechars.length);
        }
        BigInteger rv = BigInteger.ZERO;
        BigInteger biBase = BigInteger.valueOf(base);
        char cv[] = val.toCharArray();
        for (int i = 0; i < cv.length; i++) {
            rv = rv.multiply(biBase);
            char cvv = cv[i];
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


    /**
     * Array of chars for generating numbers in alternate bases.
     */
    static final char basechars[] = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-_".toCharArray();
    static final BigInteger bigIntegerChars[] = new BigInteger[basechars.length];
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
     * Public pre-generated Random for what ailes you.
     */
    public static final Random random = new Random(System.currentTimeMillis());

    /**
     * Converts a long to a base-encoded string. Does not handle negative
     * numbers;
     */
    public static String toBase(long val, int base) {
        return toBase(val, base, -1);
    }

    public static String toBase(long val, int base, int minlen) {
        if (base > basechars.length) {
            throw new RuntimeException(base + " outside base range of 2-" + basechars.length);
        }
        char out[] = new char[128];
        for (int i = out.length - 1;; i--) {
            out[i] = basechars[(int) Math.abs(val % base)];
            val = val / base;
            if (--minlen <= 0 && (val == 0 || i == 0)) {
                return new String(out, i, out.length - i);
            }
        }
    }

    public static String toBase(BigInteger val, int base) {
        return toBase(val, base, -1);
    }

    public static String toBase(BigInteger val, int base, int minlen) {
        if (base > basechars.length) {
                throw new RuntimeException(base + " outside base range of 2-" + basechars.length);
        }
        char out[] = new char[128];
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

    public static String toBase36(long val) {
        return toBase(val, 36).toUpperCase();
    }

    public static int intFromBase36(String val) {
        return intFromBase(val.toLowerCase(), 36);
    }

    public static long longFromBase36(String val) {
        return longFromBase(val.toLowerCase(), 36);
    }

    public static String toBase36(BigInteger val) {
        return toBase(val, 36).toUpperCase();
    }

    public static BigInteger bigIntegerFromBase36(String val) {
        return bigIntegerFromBase(val, 36);
    }

    public static String toBase64(long val) {
        return toBase(val, 64);
    }

    public static String toBase64(BigInteger val) {
        return toBase(val, 64);
    }

    public static BigInteger bigIntegerFromBase64(String val) {
        return bigIntegerFromBase(val.toLowerCase(), 64);
    }

    public static int intFromBase64(String val) {
        return intFromBase(val, 64);
    }


    public static long longFromBase64(String val) {
        return longFromBase(val, 64);
    }

    public static void main(String[] args) {
        System.out.println((int) '-');
        System.out.println((int) '_');
        System.out.println("A:" + (int) 'A');
        System.out.println("'A' + 36" + ('A' + 36));

        System.out.println(intFromBase("10", 64));
        System.out.println(toBase(0, 64));
        System.out.println(toBase(10, 64));
        System.out.println(toBase(63, 64));
        System.out.println(toBase(64, 64));
        System.out.println(toBase(62, 64));
        System.out.println(toBase(Integer.MAX_VALUE, 64));
        System.out.println(toBase(352345324, 64));
        System.out.println(toBase(Integer.MAX_VALUE, 36));
        System.out.println(toBase(352345324, 36));

    }

}
