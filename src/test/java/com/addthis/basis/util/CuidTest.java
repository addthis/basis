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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CuidTest {
    @Test
    public void testCreateCuid() {
        String cuid = CUID.createCUID(29823298, Numbers.random.nextInt());
        assertEquals("747f", cuid.substring(0, cuid.length() - 8));

        cuid = CUID.createCUID(1226509443755L, Numbers.random.nextInt());
        assertEquals("491b0c83", cuid.substring(0, cuid.length() - 8));

        cuid = CUID.createCUID(1226509443755L, -1);
        assertEquals("491b0c83ffffffff", cuid);

        cuid = CUID.createCUID(1226509443755L, Integer.MIN_VALUE);
        assertEquals("491b0c8380000000", cuid);

        cuid = CUID.createCUID(1226509443755L, Integer.MAX_VALUE);
        assertEquals("491b0c837fffffff", cuid);

        cuid = CUID.createCUID(Long.MIN_VALUE, Integer.MIN_VALUE);
        assertEquals("5a1cac0980000000", cuid);

        cuid = CUID.createCUID(Long.MAX_VALUE, Integer.MAX_VALUE);
        assertEquals("a5e353f77fffffff", cuid);

        cuid = CUID.createCUID(0, -1);
        assertEquals("ffffffff", cuid);

        cuid = CUID.createCUID(0, 0);
        assertEquals("0", cuid);
    }

    @Test
    public void testEncodeDecode() {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++) {
            String cuid = CUID.createCUID();
            assertEquals(16, cuid.length());
            assertTrue(cuid.matches("[0-9a-f]+"));
            long cuidTime = CUID.cuidToTime(cuid);
            long now = System.currentTimeMillis();
            assertTrue(cuidTime + 999 >= start && cuidTime <= now);
            start = now;
        }
    }
}
