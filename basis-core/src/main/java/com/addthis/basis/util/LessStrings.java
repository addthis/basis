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

import java.io.UnsupportedEncodingException;

import java.net.URLDecoder;
import java.net.URLEncoder;

import java.util.Objects;

public final class LessStrings {
    public static final String pad = "                                                      ";
    public static final String pad0 = "0000000000000000000000000000000000000000000000000000000";
    private static final int maxDecodingRounds = 20;

    private LessStrings() {}

    public static String padright(String str, int len) {
        return padright(str, len, pad);
    }

    public static String padright(String str, int len, String padder) {
        if (str.length() < len) {
            return str.concat(pad.substring(pad.length() - len + str.length()));
        } else if (str.length() > len) {
            return str.substring(0, len);
        } else {
            return str;
        }
    }

    public static String padleft(String str, int len) {
        return padleft(str, len, pad);
    }

    public static String padleft(String str, int len, String padder) {
        if (str.length() < len) {
            return padder.substring(padder.length() - len + str.length()).concat(str);
        } else if (str.length() > len) {
            return str.substring(0, len);
        } else {
            return str;
        }
    }

    public static String repeat(char c, int times) {
        StringBuilder sb = new StringBuilder(times);
        for (int i = 0; i < times; i++) {
            sb.append(c);
        }
        return sb.toString();
    }

    public static String repeat(CharSequence s, int times) {
        StringBuilder sb = new StringBuilder(s.length() * times);
        for (int i = 0; i < times; i++) {
            sb.append(s);
        }
        return sb.toString();
    }

    public static StringSplitter split(String str, String sep) {
        return new StringSplitter(str, sep);
    }

    public static StringSplitter split(String str, String sep, boolean includeTok) {
        return new StringSplitter(str, sep, includeTok);
    }

    public static String[] splitArray(String str, String sep) {
        StringSplitter ss = new StringSplitter(str, sep);
        String[] s = new String[ss.countTokens()];
        for (int i = 0; i < s.length; i++) {
            s[i] = ss.nextToken();
        }
        return s;
    }

    public static boolean isEqual(String s1, String s2) {
        return Objects.equals(s1, s2);
    }

    public static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }

    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    public static boolean isBlank(String str) {
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == ' ' || c == '\n' || c == '\r' || c == '\t') {
                return false;
            }
        }
        return true;
    }

    /**
     * returns true if all characters are ascii printable
     */
    public static boolean isASCIIPrintable(String str) {
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            if (ch < 33 || ch > 127) {
                return false;
            }
        }
        return true;
    }

    public static boolean contains(String[] haystack, String needle) {
        return contains(haystack, needle, false);
    }

    public static boolean contains(String[] haystack, String needle, boolean ignoreCase) {
        for (String hay : haystack) {
            if (hay != null && (ignoreCase ? needle.equalsIgnoreCase(hay) : needle.equals(hay))) {
                return true;
            }
        }
        return false;
    }

    /**
     * String concat utility
     */
    public static String cat(String s1, String s2) {
        return s1.concat(s2);
    }

    public static String cat(String s1, String s2, String s3) {
        return s1.concat(s2).concat(s3);
    }

    public static String cat(String s1, String s2, String s3, String s4) {
        return s1.concat(s2).concat(s3).concat(s4);
    }

    public static String cat(String s1, String s2, String s3, String s4, String s5) {
        return s1.concat(s2).concat(s3).concat(s4).concat(s5);
    }

    public static String cat(String s1, String s2, String s3, String s4, String s5, String s6) {
        return s1.concat(s2).concat(s3).concat(s4).concat(s5).concat(s6);
    }

    public static String cat(String s1, String s2, String s3, String s4, String s5, String s6, String s7) {
        return s1.concat(s2).concat(s3).concat(s4).concat(s5).concat(s6).concat(s7);
    }

    public static String cat(String s1, String s2, String s3, String s4, String s5, String s6, String s7, String s8) {
        return s1.concat(s2).concat(s3).concat(s4).concat(s5).concat(s6).concat(s7).concat(s8);
    }

    public static String cat(String s1, String s2, String s3, String s4, String s5, String s6, String s7, String s8,
                             String s9) {
        return s1.concat(s2).concat(s3).concat(s4).concat(s5).concat(s6).concat(s7).concat(s8).concat(s9);
    }

    public static String cat(String s1, String s2, String s3, String s4, String s5, String s6, String s7, String s8,
                             String s9, String s10) {
        return s1.concat(s2).concat(s3).concat(s4).concat(s5).concat(s6).concat(s7).concat(s8).concat(s9).concat(s10);
    }

    public static String cat(String s1, String s2, String s3, String s4, String s5, String s6, String s7, String s8,
                             String s9, String s10, String s11) {
        return s1.concat(s2).concat(s3).concat(s4).concat(s5).concat(s6).concat(s7).concat(s8).concat(s9).concat(s10)
                .concat(s11);
    }

    public static String cat(String s1, String s2, String s3, String s4, String s5, String s6, String s7, String s8,
                             String s9, String s10, String s11, String s12) {
        return s1.concat(s2).concat(s3).concat(s4).concat(s5).concat(s6).concat(s7).concat(s8).concat(s9).concat(s10)
                .concat(s11).concat(s12);
    }

    public static String cat(String s1, String s2, String s3, String s4, String s5, String s6, String s7, String s8,
                             String s9, String s10, String s11, String s12, String s13) {
        return s1.concat(s2).concat(s3).concat(s4).concat(s5).concat(s6).concat(s7).concat(s8).concat(s9).concat(s10)
                .concat(s11).concat(s12).concat(s13);
    }

    public static String cat(String s1, String s2, String s3, String s4, String s5, String s6, String s7, String s8,
                             String s9, String s10, String s11, String s12, String s13, String s14) {
        return s1.concat(s2).concat(s3).concat(s4).concat(s5).concat(s6).concat(s7).concat(s8).concat(s9).concat(s10)
                .concat(s11).concat(s12).concat(s13).concat(s14);
    }

    public static String cat(String s1, String s2, String s3, String s4, String s5, String s6, String s7, String s8,
                             String s9, String s10, String s11, String s12, String s13, String s14, String s15) {
        return s1.concat(s2).concat(s3).concat(s4).concat(s5).concat(s6).concat(s7).concat(s8).concat(s9).concat(s10)
                .concat(s11).concat(s12).concat(s13).concat(s14).concat(s15);
    }

    public static String cat(String s1, String s2, String s3, String s4, String s5, String s6, String s7, String s8,
                             String s9, String s10, String s11, String s12, String s13, String s14, String s15, String s16) {
        return s1.concat(s2).concat(s3).concat(s4).concat(s5).concat(s6).concat(s7).concat(s8).concat(s9).concat(s10)
                .concat(s11).concat(s12).concat(s13).concat(s14).concat(s15).concat(s16);
    }

    public static String cat(String s1, String s2, String s3, String s4, String s5, String s6, String s7, String s8,
                             String s9, String s10, String s11, String s12, String s13, String s14, String s15, String s16, String s17) {
        return s1.concat(s2).concat(s3).concat(s4).concat(s5).concat(s6).concat(s7).concat(s8).concat(s9).concat(s10)
                .concat(s11).concat(s12).concat(s13).concat(s14).concat(s15).concat(s16).concat(s17);
    }

    /** */
    public static String join(Object[] s, String j) {
        StringBuilder sb = new StringBuilder();
        for (Object p : s) {
            if (sb.length() > 0) {
                sb.append(j);
            }
            sb.append(p != null ? p.toString() : "");
        }
        return sb.toString();
    }

    /**
     * String trim utility
     */
    public static String trim(String s) {
        return s == null ? null : s.trim();
    }

    /**
     * shortens s if it is longer than len
     */
    public static String trunc(String s, int len) {
        return s == null ? null : (s.length() > len) ? s.substring(0, len) : s;
    }

    /**
     * ToString
     */
    public static String toString(Object o) {
        return o == null ? "NULL" : o.toString();
    }

    public static String printable(String raw) {
        if (raw == null) {
            return "";
        }
        byte[] nmsg = LessBytes.toBytes(raw);
        for (int i = 0; i < nmsg.length; i++) {
            if (nmsg[i] < 32 || nmsg[i] > 126) {
                nmsg[i] = '_';
            }
        }
        return LessBytes.toString(nmsg);
    }

    public static String printable(byte[] msg) {
        if (msg == null) {
            return "";
        }
        byte[] nmsg = new byte[msg.length];
        for (int i = 0; i < msg.length; i++) {
            nmsg[i] = (msg[i] < 32 || msg[i] > 126) ? (byte) '_' : msg[i];
        }
        return LessBytes.toString(nmsg);
    }

    /**
     * utility method for cleaning EOL characters in input strings
     *
     * @param source
     * @return
     */
    public static String cleanEOL(String source) {
        if (source == null) {
            return null;
        }
        return source.replaceAll("\r\n", "\n");
    }

    public static String remove(String s, char c) {
        char[] sa = s.toCharArray();
        int j = 0;
        for (int i = 0; i < sa.length; i++) {
            if (sa[i] != c) {
                sa[j++] = sa[i];
            }
        }
        return new String(sa, 0, j);
    }

    /**
     * Replace special characters with XML escapes:
     * stolen from org.json.XML.java - recopied here to add apostrophe escaping
     * <p/>
     * <pre>
     * &amp; <small>(ampersand)</small> is replaced by &amp;amp;
     * &lt; <small>(less than)</small> is replaced by &amp;lt;
     * &gt; <small>(greater than)</small> is replaced by &amp;gt;
     * &quot; <small>(double quote)</small> is replaced by &amp;quot;
     * &apos; <small>(apostrophe)</small> is replaced by &amp;apos;
     * </pre>
     *
     * @param string The string to be escaped.
     * @return The escaped string.
     */
    public static String xmlEscape(String string) {
        if (LessStrings.isEmpty(string)) {
            return string;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0, len = string.length(); i < len; i++) {
            char c = string.charAt(i);
            switch (c) {
                case '&':
                    sb.append("&amp;");
                    break;
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                case '\'':
                    sb.append("&apos;");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * URLEncoder helper, defaults to UTF-8 and eats encode exception
     *
     * @param string
     * @return url encoded version of string
     */
    public static String urlEncode(String string) {
        try {
            return URLEncoder.encode(string, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    /**
     * URLDecoder helper, defaults to UTF-8 and eats encode exception
     *
     * @param string
     * @return url encoded version of string
     */
    public static String urlDecode(String string) {
        try {
            return URLDecoder.decode(string, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    /**
     * Apply URLDecode until there is no change.
     *
     * @param encoded Possibly URL encoded string
     * @return Fully URL decoded string
     */
    public static String superURLDecode(String encoded) {

        String decoded = urlDecode(encoded);
        int rounds = 0;
        while (encoded != decoded && rounds < maxDecodingRounds) {
            encoded = decoded;
            decoded = urlDecode(encoded);
            rounds++;
        }

        return decoded;
    }

    /**
     * same as urlEncode but represents space with %20 instead of +
     * + is part of the rfc 1738 spec which was obsoleted by rfc 3986
     * http://www.ietf.org/rfc/rfc3986.txt
     * http://tools.ietf.org/html/rfc1738
     *
     * @param string
     * @return TODO - this is a cheap hack, use a better url encoder
     *         <p/>
     *         http://code.google.com/apis/gdata/javadoc/com/google/gdata/util/httputil
     *         /FastURLEncoder.html
     */
    public static String urlEncode3986(String string) {
        String str = urlEncode(string);
        return str == null ? null : str.replaceAll("\\+", "%20");
    }

    /**
     * Makes sure the supplied attribute name is in UpperCamelCase
     */
    public static String capitalize(String s) {
        if (Character.isUpperCase(s.charAt(0))) {
            return s;
        }

        StringBuilder str = new StringBuilder();
        str.append(s.substring(0, 1).toUpperCase());
        str.append(s.substring(1));
        return str.toString();
    }

    /**
     * search str, starting at fromIndex and look for any character in vals.
     * returns str.length if no match, useful for substring operations where
     * you're searching for the end of a subsequence and prefer strlen to -1
     * when there is no match
     *
     * @param str
     * @param fromIndex
     * @param vals      characters to search for
     * @return the index of the first occurrence of any character in vals in
     *         str starting at fromIndex, or, return str.length() if not found
     */
    public static int indexOfOrLength(String str, int fromIndex, char... vals) {
        while (fromIndex < str.length()) {
            for (char val : vals) {
                if (str.charAt(fromIndex) == val) {
                    return fromIndex;
                }
            }
            fromIndex++;
        }
        return str.length();
    }

    /**
     * return an empty string if str is null, else return str.  useful for templating, jdom and jsp which don't like nulls.
     *
     * @param str
     * @return
     */
    public static String unNull(String str) {
        return str == null ? "" : str;
    }
}
