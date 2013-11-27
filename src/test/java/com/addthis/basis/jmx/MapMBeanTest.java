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
package com.addthis.basis.jmx;

import java.util.HashMap;
import java.util.Map;

import javax.management.Attribute;
import javax.management.AttributeList;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MapMBeanTest {


    @Test
    public void testInit() {
        // Make sure nothing explodes.
        Map<String, String> map = new HashMap<String, String>();
        MapMBean mbean = new MapMBean(map);
    }


    @Test
    public void testGettingAttributes() {
        Map<String, String> testMap = new HashMap<String, String>();
        testMap.put("key0", "val0");
        MapMBean mbean = new MapMBean(testMap);
        assertEquals("val0",
                mbean.getAttribute("key0"));
        AttributeList al = mbean.getAttributes(new String[]{"key0"});
        assertEquals(1, al.size());
        assertEquals(new Attribute("key0", "val0"), al.get(0));
    }


    @Test
    public void nullHandling() {
        MapMBean mbean = new MapMBean(null);
        AttributeList al = mbean.getAttributes(new String[]{"key0"});
        assertEquals(0, al.size());
    }

}
