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

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import junit.framework.TestCase;

public class MBeanSetTest extends TestCase {
    String domain = UUID.randomUUID().toString();
    MBeanListener listener = null;

    public void testSuccess() throws Exception {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();

        List<Test> pre = new LinkedList<Test>();
        for (int i = 0; i < 3; i++) {
            add(pre);
        }

        List<Test> post = new LinkedList<Test>();

        final Set<ObjectName> set = new HashSet<ObjectName>();

        MBeanUtils.listen(listener = new MBeanListener() {
            @Override
            public void mbeanAdded(ObjectName name) {
                set.add(name);
            }

            @Override
            public void mbeanRemoved(ObjectName name) {
                set.remove(name);
            }
        }, new ObjectName(domain + ":*"));

        assertEquals(pre.size(), set.size());
        for (Test t : pre) {
            assertTrue(set.contains(t.name));
        }

        for (int i = 0; i < post.size(); i++) {
            add(post);
        }

        assertEquals(pre.size() + post.size(), set.size());
        for (Test t : pre) {
            assertTrue(set.contains(t.name));
        }
        for (Test t : post) {
            assertTrue(set.contains(t.name));
        }

        for (int i = 0; i < post.size(); i++) {
            server.unregisterMBean(post.get(i).name);
        }

        assertEquals(pre.size(), set.size());
        for (Test t : pre) {
            assertTrue(set.contains(t.name));
        }

        MBeanUtils.unlisten(listener);
        set.clear();

        assertEquals(0, set.size());
        for (int i = 0; i < post.size(); i++) {
            add(post);
        }
        assertEquals(0, set.size());
    }

    protected void add(List<Test> list) throws Exception {
        Test t = new Test();
        t.id = UUID.randomUUID().toString();
        t.name = new ObjectName(domain + ":id=" + t);
        list.add(t);
        ManagementFactory.getPlatformMBeanServer().registerMBean(t, t.name);
    }

    public static interface TestMBean {
    }

    public static class Test implements TestMBean {
        public String id;
        public ObjectName name;
    }
}
