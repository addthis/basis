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

import com.google.common.hash.Hashing;
import com.google.common.hash.HashFunction;

public class Numbers {
    private static final HashFunction fallbackHashFunc = Hashing.murmur3_128();


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
        char cv[] = val.toCharArray();
        for (char aCv : cv) {
            rv *= base;
            int av = 0;
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
        char cv[] = val.toCharArray();
        for (char aCv : cv) {
            rv *= base;
            if (aCv >= '0' && aCv <= '9') {
                rv += aCv - '0';
            } else if (aCv >= 'a' && aCv <= 'z') {
                rv += aCv - 'a' + 10;
            } else if (aCv >= 'A' && aCv <= 'Z') {
                rv += aCv - 'A' + 36;
            } else {
                throw new RuntimeException("invalid base encoding: " + val);
            }
        }
        return rv;
    }

    /**
     * Array of chars for generating numbers in alternate bases.
     */
    static final char basechars[] = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    public static final int MAX_BASE = basechars.length;
    /**
     * String containing zeros for pre-padding base-36 longs.
     */
    private static final String pads[] = new String[MAX_BASE];
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
        char out[] = new char[128];
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
                pad = Bytes.clear(toBase(Long.MAX_VALUE, base), '0');
                pads[base - 1] = pad;
            }
            String nv = toBase(random.nextLong(), base);
            return pad.substring(nv.length()) + nv;
        }
    }

    /**
     * Array of bytes for generating hex strings.
     */
    public static final byte hex[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    public static final byte HEX[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    private static final boolean verboseHex = System.getProperty("xhex", "0").equals("1");

    public static String toHexString(byte b[]) {
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
}
