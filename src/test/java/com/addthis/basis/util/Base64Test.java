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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;


public class Base64Test {
    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testEncodeString() {
        String decoded = "foo";
        String encoded = "Zm9v";
        assertEquals(encoded, Base64.encode(decoded));
    }

    @Test
    public void testEncodeString_NonURLSafeString() {
        String decoded = " ";
        String encoded = "IA==";
        assertEquals(encoded, Base64.encode(decoded));
    }

    @Test
    public void testEncodeString_URLSafeString() {
        String decoded = " ";
        String encoded = "IA==";
        assertEquals(encoded, Base64.encodeURLSafe(decoded));
    }

    @Test
    public void testEncodeByteArray() {
        byte[] decoded = new byte[]{7, 9, 7};
        byte[] encoded = new byte[]{66, 119, 107, 72};
        assertArrayEquals(encoded, new String(Base64.encode(decoded)).getBytes());
    }

    @Test
    public void testEncodeURLSafeByteArray() {
        byte[] decoded = new byte[]{7, 9, 7};
        byte[] encoded = new byte[]{66, 119, 107, 72};
        assertArrayEquals(encoded, new String(Base64.encodeURLSafe(decoded)).getBytes());
    }

    @Test
    public void testDecodeString() {
        String decoded = "foo";
        String encoded = "Zm9v";
        assertEquals(decoded, Base64.decode(encoded));
    }

    @Test
    public void testDecodeString_NonURLSafeString() {
        String decoded = " ";
        String encoded = "IA==";
        assertEquals(decoded, Base64.decode(encoded));
    }

    @Test
    public void testDecodeString_URLSafeString() {
        String decoded = " ";
        String encoded = "IA==";
        assertEquals(decoded, Base64.decodeURLSafe(encoded));
    }

    public void testStringRoundTrip() {
        String input = "foobar baz qux";
        String encoded = Base64.encode(input);
        assertEquals(input, Base64.decode(encoded));
    }

    @Test
    public void testDecodeCharArray() {
        byte[] decoded = new byte[]{7, 9, 7};
        char[] encoded = new char[]{66, 119, 107, 72};
        assertArrayEquals(decoded, Base64.decode(encoded));
    }

    @Test
    public void testDecodeURLSafeCharArray() {
        byte[] decoded = new byte[]{7, 9, 7};
        char[] encoded = new char[]{66, 119, 107, 72};
        assertArrayEquals(decoded, Base64.decodeURLSafe(encoded));
    }
}
