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

public class ParameterTest {

    @Test
    public void testValue() {
        System.setProperty("val1", "bar");
        assertEquals("bar", Parameter.value("val1"));
        assertEquals(null, Parameter.value("baz"));
        assertEquals("quux", Parameter.value("baz", "quux"));
    }

    @Test
    public void testBoolValue() {
        System.setProperty("bool1", "1");
        System.setProperty("bool2", "0");
        System.setProperty("bool3", "true");
        assertEquals(true, Parameter.boolValue("bool1", true));
        assertEquals(true, Parameter.boolValue("bool1", false));
        assertEquals(false, Parameter.boolValue("bool2", true));
        assertEquals(false, Parameter.boolValue("bool2", false));
        assertEquals(true, Parameter.boolValue("bool3", true));
        assertEquals(true, Parameter.boolValue("bool3", false));
        assertEquals(true, Parameter.boolValue("baz", true));
        assertEquals(false, Parameter.boolValue("baz", false));

        boolean success = false;

        try {
            Parameter.boolValue("", true);
        } catch (IllegalArgumentException ex) {
            success = true;
        }
        assertTrue(success);

        success = false;

        try {
            Parameter.boolValue(null, true);
        } catch (NullPointerException ex) {
            success = true;
        }
        assertTrue(success);

    }

    @Test
    public void testIntValue() {
        System.setProperty("int1", "1GB");
        System.setProperty("int2", "1GiB");
        System.setProperty("int3", "1MB");
        System.setProperty("int4", "1MiB");
        System.setProperty("int5", "1KB");
        System.setProperty("int6", "1KiB");
        assertEquals(1024 * 1024 * 1024, Parameter.intValue("int1", 0));
        assertEquals(1000 * 1000 * 1000, Parameter.intValue("int2", 0));
        assertEquals(1024 * 1024, Parameter.intValue("int3", 0));
        assertEquals(1000 * 1000, Parameter.intValue("int4", 0));
        assertEquals(1024, Parameter.intValue("int5", 0));
        assertEquals(1000, Parameter.intValue("int6", 0));
        assertEquals(0, Parameter.intValue("baz", 0));

        boolean success = false;

        try {
            Parameter.intValue("", 0);
        } catch (IllegalArgumentException ex) {
            success = true;
        }
        assertTrue(success);

        success = false;

        try {
            Parameter.intValue(null, 0);
        } catch (NullPointerException ex) {
            success = true;
        }
        assertTrue(success);


    }

}
