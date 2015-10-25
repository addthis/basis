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

import java.lang.management.ManagementFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import javax.management.Attribute;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import junit.framework.TestCase;

public class FieldBasedDynamicMBeanTest extends TestCase {
    MBeanServer server;
    ObjectName name;
    Bean bean;
    Random random;

    public void setUp() throws Exception {
        name = new ObjectName("test:name=" + getName());
        server = ManagementFactory.getPlatformMBeanServer();
        bean = new Bean();
        random = new Random();

        MBeanUtils.register(name, bean);
    }

    public void tearDown() throws Exception {
        MBeanUtils.unregister(name);
    }

    public void testMBeanInfo() throws Exception {
        MBeanInfo i = server.getMBeanInfo(name);

        assertNotNull(i);
        assertNotNull(i.getAttributes());
        assertEquals(2, i.getAttributes().length);

        Map<String, Class<?>> atts = new HashMap<String, Class<?>>();
        atts.put("foo", String.class);
        atts.put("bar", int.class);

        for (MBeanAttributeInfo a : i.getAttributes()) {
            Class<?> c = atts.remove(a.getName());
            assertNotNull(c);
            assertEquals(c.getName(), a.getType());
            assertTrue(a.isReadable());
            assertTrue(a.isWritable());
        }

        assertEquals(0, atts.size());
    }

    public void testGet() throws Exception {
        for (int i = 0; i < 100; i++) {
            bean.foo = UUID.randomUUID().toString();
            bean.bar = random.nextInt();

            assertEquals(bean.foo, server.getAttribute(name, "foo"));
            assertEquals(bean.bar, server.getAttribute(name, "bar"));
        }
    }

    public void testGetAndSet() throws Exception {
        for (int i = 0; i < 100; i++) {
            String s1 = UUID.randomUUID().toString();
            server.setAttribute(name, new Attribute("foo", s1));
            assertEquals(s1, server.getAttribute(name, "foo"));
            assertEquals(s1, bean.foo);
        }

        for (int i = 0; i < 100; i++) {
            String s1 = UUID.randomUUID().toString();
            bean.foo = s1;
            assertEquals(s1, server.getAttribute(name, "foo"));
            assertEquals(s1, bean.foo);
        }
    }

    public static class Bean extends FieldBasedDynamicMBean {
        public String foo;
        public int bar;

        public Bean() {
            super(false);
        }
    }
}
