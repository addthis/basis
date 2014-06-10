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

import com.google.common.io.BaseEncoding;

// NOTE: old strict param is ignored
public class Base64 {
    private static final BaseEncoding base64 = BaseEncoding.base64();
    private static final BaseEncoding base64Url = BaseEncoding.base64Url();


    public static void main(String args[]) {
        System.out.println("decode(" + args[0] + ") = " + decode(args[0]));
        System.out.println("encode(" + args[0] + ") = " + encode(args[0]));
    }


    /**
     * Encodes a string into Base64 format.
     * No blanks or line breaks are inserted.
     *
     * @param s a String to be encoded.
     * @return A String with the Base64 encoded data.
     */
    public static String encode(String s) {
        return base64.encode(Bytes.toBytes(s));
    }

    public static String encodeURLSafe(String s) {
        return base64Url.encode(Bytes.toBytes(s));
    }

    /**
     * Encodes a byte array into Base64 format.
     * No blanks or line breaks are inserted.
     *
     * @param in an array containing the data bytes to be encoded.
     * @return A character array with the Base64 encoded data.
     */
    public static char[] encode(byte[] in) {
        return base64.encode(in).toCharArray();
    }

    public static char[] encodeURLSafe(byte[] in) {
        return base64Url.encode(in).toCharArray();
    }


    /**
     * Decodes a Base64 string.
     *
     * @param s a Base64 String to be decoded.
     * @return A String containing the decoded data.
     * @throws IllegalArgumentException if the input is not valid Base64 encoded data.
     */
    public static String decode(String s) {
        return Bytes.toString(base64.decode(s));
    }

    public static String decode(String s, boolean strict) {
        return decode(s);
    }

    public static String decodeURLSafe(String s) {
        return Bytes.toString(base64Url.decode(s));
    }

    public static String decodeURLSafe(String s, boolean strict) {
        return decodeURLSafe(s);
    }

    /**
     * Decodes Base64 data.
     * No blanks or line breaks are allowed within the Base64 encoded data.
     *
     * @param in a character array containing the Base64 encoded data.
     * @return An array containing the decoded data bytes.
     * @throws IllegalArgumentException if the input is not valid Base64 encoded data.
     */
    public static byte[] decode(char[] in) {
        return base64.decode(new String(in));
    }

    public static byte[] decode(char[] in, boolean strict) {
        return decode(in);
    }

    public static byte[] decodeURLSafe(char[] in) {
        return base64Url.decode(new String(in));
    }

    public static byte[] decodeURLSafe(char[] in, boolean strict) {
        return decodeURLSafe(in);
    }

}
