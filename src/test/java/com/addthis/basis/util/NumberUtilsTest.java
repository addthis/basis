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
import java.util.UUID;

import junit.framework.TestCase;


public class NumberUtilsTest extends TestCase {

    public void testDecode() {
        assertEquals(0, NumberUtils.intFromBase36("000000"));
        assertEquals(1, NumberUtils.intFromBase36("000001"));
        assertEquals(623714775, NumberUtils.intFromBase36("ABCDEF"));
        assertEquals(22, NumberUtils.intFromBase36("M"));
        assertEquals(35, NumberUtils.intFromBase36("Z"));
        assertEquals(0, NumberUtils.intFromBase36("0"));
        assertEquals(0, NumberUtils.intFromBase36("0"));
        assertEquals(22, NumberUtils.intFromBase36("M"));
        assertEquals(481261, NumberUtils.intFromBase36("abcd"));

        assertEquals(0, NumberUtils.bigIntegerFromBase36("000000").intValue());
        assertEquals(1, NumberUtils.bigIntegerFromBase36("000001").intValue());
        assertEquals(623714775, NumberUtils.bigIntegerFromBase36("ABCDEF").intValue());
        assertEquals(22, NumberUtils.bigIntegerFromBase36("M").intValue());
        assertEquals(35, NumberUtils.bigIntegerFromBase36("Z").intValue());
        assertEquals(0, NumberUtils.bigIntegerFromBase36("0").intValue());
        assertEquals(0, NumberUtils.bigIntegerFromBase36("0").intValue());
        assertEquals(22, NumberUtils.bigIntegerFromBase36("M").intValue());
        assertEquals(481261, NumberUtils.bigIntegerFromBase36("abcd").intValue());
    }

    public void testEncode() {
        assertEquals("0", NumberUtils.toBase36(0));
        assertEquals("1", NumberUtils.toBase36(1));
        assertEquals("1Q", NumberUtils.toBase36(62));
        assertEquals("1R", NumberUtils.toBase36(63));
        assertEquals("ZIK0ZJ", NumberUtils.toBase36(Integer.MAX_VALUE));

        assertEquals("-", NumberUtils.toBase64(62));
        assertEquals("_", NumberUtils.toBase64(63));
        assertEquals("10", NumberUtils.toBase64(64));
        assertEquals("L4_O", NumberUtils.toBase64(12341234));
        assertEquals("1_____", NumberUtils.toBase64(Integer.MAX_VALUE));

        assertEquals("0", NumberUtils.toBase36(BigInteger.valueOf(0)));
        assertEquals("1", NumberUtils.toBase36(BigInteger.valueOf(1)));
        assertEquals("1Q", NumberUtils.toBase36(BigInteger.valueOf(62)));
        assertEquals("1R", NumberUtils.toBase36(BigInteger.valueOf(63)));
        assertEquals("ZIK0ZJ", NumberUtils.toBase36(BigInteger.valueOf(Integer.MAX_VALUE)));

        assertEquals("-", NumberUtils.toBase64(BigInteger.valueOf(62)));
        assertEquals("_", NumberUtils.toBase64(BigInteger.valueOf(63)));
        assertEquals("10", NumberUtils.toBase64(BigInteger.valueOf(64)));
        assertEquals("L4_O", NumberUtils.toBase64(BigInteger.valueOf(12341234)));
        assertEquals("1_____", NumberUtils.toBase64(BigInteger.valueOf(Integer.MAX_VALUE)));
    }


    public void testBigIntegers() {
        BigInteger val = BigInteger.valueOf(Long.MAX_VALUE);
        assertEquals(val, NumberUtils.bigIntegerFromBase36(NumberUtils.toBase36(val)));
        assertEquals(val, NumberUtils.bigIntegerFromBase64(NumberUtils.toBase64(val)));
        val = val.add(BigInteger.ONE);
        assertEquals(val, NumberUtils.bigIntegerFromBase36(NumberUtils.toBase36(val)));
        assertEquals(val, NumberUtils.bigIntegerFromBase64(NumberUtils.toBase64(val)));
        val = val.shiftLeft(64);
        val = val.add(BigInteger.valueOf(Long.MAX_VALUE));
        assertEquals(val, NumberUtils.bigIntegerFromBase36(NumberUtils.toBase36(val)));
        assertEquals(val, NumberUtils.bigIntegerFromBase64(NumberUtils.toBase64(val)));
     }

    public void testUUID() {
        Random random = new Random();
        for(int i = 0; i < 1_000_000; i++) {
            long hiBits = random.nextLong();
            long loBits = random.nextLong();
            UUID uuid = new UUID(hiBits, loBits);
            assertEquals(uuid, NumberUtils.UUIDFromBase64(NumberUtils.toBase64(uuid)));
        }
    }


    public void testA() {
        assertEquals(0, NumberUtils.intFromBase64("000000"));
        assertEquals(1, NumberUtils.intFromBase64("000001"));
        assertEquals(630880809, NumberUtils.intFromBase64("ABCDEF"));
    }

}
