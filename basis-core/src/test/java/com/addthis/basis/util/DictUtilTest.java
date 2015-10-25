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

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DictUtilTest {

    @Test
    public void parseTest() {
        Map<String, String> map = new LinkedHashMap<String, String>();
        assertEquals(map, DictUtil.parse(""));
        map.put("key1", "val1");
        assertEquals(map, DictUtil.parse("key1=val1"));
        map.put("key2", "val 2");
        assertEquals(map, DictUtil.parse("key1=val1&key2=val+2"));
        map.clear();
        map.put("key", null);
        assertEquals("value missing parse test mismatch", map, DictUtil.parse("key"));
        map.clear();
        map.put("key", "");
        assertEquals("empty value parse test mismatch", map, DictUtil.parse("key="));
    }

    @Test
    public void toStringTest() {
        Map<String, String> map = new LinkedHashMap<String, String>();
        assertEquals("", DictUtil.toString(map));
        map.put("key1", "val1");
        assertEquals("key1=val1", DictUtil.toString(map));
        map.put("key2", "val 2");
        assertEquals("key1=val1&key2=val+2", DictUtil.toString(map));
        map.clear();
        map.put("key", null);
        assertEquals("value missing parse test mismatch", "key", DictUtil.toString(map));
        map.clear();
        map.put("key", "");
        assertEquals("empty value parse test mismatch", "key=", DictUtil.toString(map));
    }
}
