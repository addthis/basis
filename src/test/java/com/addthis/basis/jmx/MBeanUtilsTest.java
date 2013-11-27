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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import junit.framework.TestCase;

public class MBeanUtilsTest extends TestCase {
    MBeanServer server;

    public void setUp() throws Exception {
        server = ManagementFactory.getPlatformMBeanServer();
    }

    public void testParse() {
        String d = UUID.randomUUID().toString();
        List<String> p = new LinkedList<String>();
        for (int i = 0; i < 10; i++) {
            p.add(UUID.randomUUID().toString());
        }

        StringBuffer str = new StringBuffer();
        str.append(d).append(":");
        for (int i = 0; i < p.size(); i++) {
            if (i > 0) {
                str.append(",");
            }
            str.append("key-" + i).append("=").append(p.get(i));
        }

        ObjectName n = MBeanUtils.parseName(str.toString());
        assertNotNull(n);
        assertEquals(d, n.getDomain());
        for (int i = 0; i < p.size(); i++) {
            assertEquals(p.get(i), n.getKeyProperty("key-" + i));
        }
    }

    public void testBuild() {
        String d = UUID.randomUUID().toString();
        List<String> p = new LinkedList<String>();
        for (int i = 0; i < 10; i++) {
            p.add(UUID.randomUUID().toString());
        }

        StringBuffer str = new StringBuffer();
        str.append(d).append(":");
        for (int i = 0; i < p.size(); i++) {
            if (i > 0) {
                str.append(",");
            }
            str.append("key-" + i).append("=").append(p.get(i));
        }

        Object[] args = new Object[2 * p.size()];
        for (int i = 0; i < args.length; i += 2) {
            args[i] = "key-" + i / 2;
            args[i + 1] = p.get(i / 2);
        }

        assertEquals(str.toString(), MBeanUtils.buildName(d, args).toString());
    }

    public void testSupports() throws Exception {
        ObjectName n = new ObjectName("foo:id=" + UUID.randomUUID().toString());
        Impl s = new Impl();
        server.registerMBean(new StandardMBean(s, Ifc2.class), n);
        assertTrue(MBeanUtils.supports(n, Ifc2.class));
        assertTrue(MBeanUtils.supports(n, CopyOfIfc2.class));
    }

    public void testProxy() throws Exception {
        ObjectName n = new ObjectName("foo:id=" + UUID.randomUUID().toString());
        Impl s = new Impl();
        server.registerMBean(new StandardMBean(s, Ifc1.class), n);

        Ifc1 s1 = MBeanUtils.createProxy(getClass().getClassLoader(), n, Ifc1.class);
        assertNotNull(s1);
        assertEquals(s.foo, s1.getfoo());
        assertEquals(s.bar, s1.getBar());
        assertEquals("ab", s1.cat("a", "b"));

        Ifc1 s2 = MBeanUtils.createProxy(getClass().getClassLoader(), n, Ifc1.class);
        assertNotNull(s2);
        assertEquals(s.foo, s2.getfoo());
        assertEquals(s.bar, s2.getBar());
        assertEquals("ab", s2.cat("a", "b"));
    }

    public void testDynamic() throws Exception {
        ObjectName n = new ObjectName("foo:id=" + UUID.randomUUID().toString());
        Impl s = new Impl();
        server.registerMBean(MBeanUtils.createDynamicMBean(s, Arrays.<Class<?>>asList(Ifc1.class, Ifc2.class), null), n);

        Ifc1 s1 = MBeanUtils.createProxy(getClass().getClassLoader(), n, Ifc1.class);
        assertNotNull(s1);
        assertEquals(s.foo, s1.getfoo());
        assertEquals(s.bar, s1.getBar());
        assertEquals("ab", s1.cat("a", "b"));

        Ifc2 s2 = MBeanUtils.createProxy(getClass().getClassLoader(), n, Ifc2.class);
        assertNotNull(s2);
        s2.setBar("blah");
        assertEquals("blah", s.bar);
        assertEquals("blah", s1.getBar());
        assertEquals("ab", s2.cat("ab"));
    }

    public static interface Ifc1 {
        public String getfoo();

        public String getBar();

        public String cat(String s1, String s2);
    }

    public static interface Ifc2 {
        public void setBar(String bar);

        public String cat(String s1);
    }

    public static interface CopyOfIfc2 {
        public void setBar(String bar);

        public String cat(String s1);
    }

    public static class Impl implements Ifc1, Ifc2 {
        public String foo = UUID.randomUUID().toString();
        public String bar = UUID.randomUUID().toString();

        @Override
        public String cat(String s1) {
            return s1;
        }

        @Override
        public String cat(String s1, String s2) {
            return s1 + s2;
        }

        @Override
        public String getBar() {
            return bar;
        }

        @Override
        public String getfoo() {
            return foo;
        }

        @Override
        public void setBar(String bar) {
            this.bar = bar;
        }
    }
}
