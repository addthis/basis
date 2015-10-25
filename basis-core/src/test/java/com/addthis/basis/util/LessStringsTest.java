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
import java.net.URLEncoder;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class LessStringsTest {
    @Test
    public void testRepeat() {
        assertEquals("", LessStrings.repeat('*', 0));
        assertEquals("*", LessStrings.repeat('*', 1));
        assertEquals("**", LessStrings.repeat('*', 2));
        assertEquals("***", LessStrings.repeat('*', 3));

        assertEquals("", LessStrings.repeat("*-", 0));
        assertEquals("*-", LessStrings.repeat("*-", 1));
        assertEquals("*-*-", LessStrings.repeat("*-", 2));
        assertEquals("*-*-*-", LessStrings.repeat("*-", 3));
    }

    @Test
    public void testSuperURLDecode_DoesntAlterDecodedString() {
        String decoded = "http://example.com/path?u=http://example.com/&val2=abc def&val3=jkl#1234";
        assertEquals(decoded, LessStrings.superURLDecode(decoded));
    }

    @Test
    public void testSuperURLDecode_HandlesSingleEncodedString() {
        String encoded = "http://example.com/path?u=http%3A%2F%2Fexample.com%2F&val2=abc+def&val3=jkl#1234";
        String decoded = "http://example.com/path?u=http://example.com/&val2=abc def&val3=jkl#1234";
        assertEquals(decoded, LessStrings.superURLDecode(encoded));
    }

    @Test
    public void testSuperURLDecode_HandlesDoubleEncodedString() {
        String encoded = "http%3A%2F%2Fexample.com%2Fpath%3Fu%3Dhttp%253A%252F%252Fexample.com%252F%26val2%3Dabc%2Bdef%26val3%3Djkl%231234";
        String decoded = "http://example.com/path?u=http://example.com/&val2=abc def&val3=jkl#1234";
        assertEquals(decoded, LessStrings.superURLDecode(encoded));
    }

    @Test
    public void testSuperURLDecode_HandlesManyEncodedString() throws UnsupportedEncodingException {
        String decoded = "http://example.com/path?u=http://example.com/&val2=abc def&val3=jkl#1234";

        String encoded = decoded;
        for (int x = 0; x < 10; x++) {
            encoded = URLEncoder.encode(encoded, "UTF-8");
        }

        assertEquals(decoded, LessStrings.superURLDecode(encoded));
    }

    @Test(expected = NegativeArraySizeException.class)
    public void testRepeatNegativeTimes() {
        LessStrings.repeat('!', -1);
    }

    @Test
    public void testIndexOfOrLength() {
        String str = "0123456789";
        // basic
        assertEquals(1, LessStrings.indexOfOrLength(str, 0, '1', '5'));
        // fromIndex
        assertEquals(1, LessStrings.indexOfOrLength(str, 1, '1', '5'));
        // fromIndex, skips first val
        assertEquals(5, LessStrings.indexOfOrLength(str, 2, '1', '5'));
        // no match
        assertEquals(str.length(), LessStrings.indexOfOrLength(str, 0, 'x'));
        // no match with from index
        assertEquals(str.length(), LessStrings.indexOfOrLength(str, 2, 'x'));
    }

    @Test
    public void testUrlEncoder() {
        String a = "my ô&test%26£str";
        String aEnc = "my%20%C3%B4%26test%2526%C2%A3str";
        String aPlus = "my+%C3%B4%26test%2526%C2%A3str";
        String b = "изображение";
        String bEnc = "%D0%B8%D0%B7%D0%BE%D0%B1%D1%80%D0%B0%D0%B6%D0%B5%D0%BD%D0%B8%D0%B5";
        assertEquals(aPlus, LessStrings.urlEncode(a));
        assertEquals(aEnc, LessStrings.urlEncode3986(a));
        assertEquals(bEnc, LessStrings.urlEncode(b));
    }

    @Test
    public void testSplit() {
        String[] res = LessStrings.splitArray("a:b::c", ":");
        assertEquals("a", res[0]);
        assertEquals("b", res[1]);
        assertEquals("c", res[2]);
    }
}
