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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LessBytesTest {
    @Test
    public void test1() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int check[] = new int[]{5, 100, 4, 400, 3, 600, 2, 1, 0, 200, 1, 700, 2, 100, 3};
        for (int i : check) {
            LessBytes.writeBytes(new byte[i], out);
        }
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        for (int i : check) {
            byte b[] = LessBytes.readBytes(in);
            Assert.assertEquals(b.length, i);
        }
    }

    @Test
    public void testUrlEncoder() throws UnsupportedEncodingException {
        System.setProperty("nativeURLCodec", "0");
        String rawString = "my ô&test%26£str";
        String testURLEnc = URLEncoder.encode(rawString, "UTF-8");
        String encodedString = LessBytes.urlencode(rawString);
        assertEquals(testURLEnc, encodedString);

        String rawString2 = "изображение";
        String testURLEnc2 = URLEncoder.encode(rawString2, "UTF-8");
        String encodedString2 = LessBytes.urlencode(rawString2);
        assertEquals(testURLEnc2, encodedString2);
    }

    @Test
    public void testUrlEncoder_native() throws UnsupportedEncodingException {
        System.setProperty("nativeURLCodec", "1");
        String rawString = "my ô&test%26£str";
        String testURLEnc = URLEncoder.encode(rawString, "UTF-8");
        String encodedString = LessBytes.urlencode(rawString);
        assertEquals(testURLEnc, encodedString);

        String rawString2 = "изображение";
        String testURLEnc2 = URLEncoder.encode(rawString2, "UTF-8");
        String encodedString2 = LessBytes.urlencode(rawString2);
        assertEquals(testURLEnc2, encodedString2);
    }

    @Test
    public void testUrlDecoder() throws UnsupportedEncodingException {
        System.setProperty("nativeURLCodec", "0");
        String rawString = "my ô&test%26£str";
        String testURLEnc = URLDecoder.decode(rawString, "UTF-8");
        String encodedString = LessBytes.urldecode(rawString);
        assertEquals(testURLEnc, encodedString);

        String rawString2 = "изображение";
        String testURLEnc2 = URLDecoder.decode(rawString2, "UTF-8");
        String encodedString2 = LessBytes.urldecode(rawString2);
        assertEquals(testURLEnc2, encodedString2);
    }

    @Test
    public void testUrlDecoder_native() throws UnsupportedEncodingException {
        System.setProperty("nativeURLCodec", "1");
        String rawString = "my ô&test%26£str";
        String testURLEnc = URLDecoder.decode(rawString, "UTF-8");
        String encodedString = LessBytes.urldecode(rawString);
        assertEquals(testURLEnc, encodedString);

        String rawString2 = "изображение";
        String testURLEnc2 = URLDecoder.decode(rawString2, "UTF-8");
        String encodedString2 = LessBytes.urldecode(rawString2);
        assertEquals(testURLEnc2, encodedString2);
    }

//    @Test
//    public void testUrlDecoder_Baseline() throws UnsupportedEncodingException {
//        String rawString = "my ô&test%26£str";
//        double startTime = System.nanoTime();
//        for (int i = 0; i < 10000000; i++) {
//            URLDecoder.decode(rawString, "UTF-8");
//        }
//        double endTime = System.nanoTime();
//        double totalTime = (endTime - startTime) / 1000000000.0;
//        System.out.println(totalTime + " seconds " + 10000000 / totalTime + "/s");
//    }

//    @Test
//    public void testUrlDecoder_Performance() throws UnsupportedEncodingException {
//        System.setProperty("nativeURLCodec", "0");
//        String rawString = "my ô&test%26£str";
//        double startTime = System.nanoTime();
//        for (int i = 0; i < 10000000; i++) {
//            Bytes.urldecode(rawString);
//        }
//        double endTime = System.nanoTime();
//        double totalTime = (endTime - startTime) / 1000000000.0;
//        System.out.println(totalTime + " seconds " + 10000000 / totalTime + "/s");
//    }
//
//    @Test
//    public void testUrlDecoder_nativePerformance() throws UnsupportedEncodingException {
//        System.setProperty("nativeURLCodec", "1");
//        String rawString = "my ô&test%26£str";
//        double startTime = System.nanoTime();
//        for (int i = 0; i < 10000000; i++) {
//            Bytes.urldecode(rawString);
//        }
//        double endTime = System.nanoTime();
//        double totalTime = (endTime - startTime) / 1000000000.0;
//        System.out.println(totalTime + " seconds " + 10000000 / totalTime + "/s");
//    }
//
//    @Test
//    public void testUrlEncoder_Baseline() throws UnsupportedEncodingException {
//        String rawString = "my ô&test%26£str";
//        double startTime = System.nanoTime();
//        for (int i = 0; i < 10000000; i++) {
//            URLEncoder.encode(rawString, "UTF-8");
//        }
//        double endTime = System.nanoTime();
//        double totalTime = (endTime - startTime) / 1000000000.0;
//        System.out.println(totalTime + " seconds " + 10000000 / totalTime + "/s");
//    }
//
//    @Test
//    public void testUrlEncoder_Performance() throws UnsupportedEncodingException {
//        System.setProperty("nativeURLCodec", "0");
//        String rawString = "my ô&test%26£str";
//        double startTime = System.nanoTime();
//        for (int i = 0; i < 10000000; i++) {
//            Bytes.urlencode(rawString);
//        }
//        double endTime = System.nanoTime();
//        double totalTime = (endTime - startTime) / 1000000000.0;
//        System.out.println(totalTime + " seconds " + 10000000 / totalTime + "/s");
//    }
//
//    @Test
//    public void testUrlEncoder_nativePerformance() throws UnsupportedEncodingException {
//        System.setProperty("nativeURLCodec", "1");
//        String rawString = "my ô&test%26£str";
//        double startTime = System.nanoTime();
//        for (int i = 0; i < 10000000; i++) {
//            Bytes.urlencode(rawString);
//        }
//        double endTime = System.nanoTime();
//        double totalTime = (endTime - startTime) / 1000000000.0;
//        System.out.println(totalTime + " seconds " + 10000000 / totalTime + "/s");
//    }

    @Test
    public void testWriteLength() throws Exception {
        for (long l = 0; l < 10000000; l += 10) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            LessBytes.writeLength(l, bos);
            long v = LessBytes.readLength(new ByteArrayInputStream(bos.toByteArray()));
            assertTrue("mismatch input=" + l + " output=" + v, l == v);
        }
    }

    /**
     * conversion between byte array and char array
     */
    @Test
    public void testConversion_bytes_chars() {
        char[] oldValues = new char[]{0x0011, 0x2233};
        // to bytes then back should result in the same values
        char[] newValues = LessBytes.toChars(LessBytes.toBytes(oldValues));
        assertArrayEquals(oldValues, newValues);
    }

    /**
     * conversion between byte array and int array
     */
    @Test
    public void testConversion_bytes_ints() {
        int[] oldValues = new int[]{0x00112233, 0x44556677};
        int[] newValues = LessBytes.toInts(LessBytes.toBytes(oldValues));
        assertArrayEquals(oldValues, newValues);
    }
}
