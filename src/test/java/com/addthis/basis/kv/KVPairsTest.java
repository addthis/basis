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
package com.addthis.basis.kv;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class KVPairsTest {
    @Test
    public void init() throws Exception {
        KVPairs kv = new KVPairs("abc=123&def=234&&xyz==pdq");
        assertEquals(kv.getValue("abc"), "123");
        assertEquals(kv.getValue("def"), "234");
        assertEquals(kv.getValue("xyz"), "=pdq");
        assertEquals(kv.toString(), "abc=123&def=234&xyz=%3Dpdq");

        kv = KVPairs.fromFullURL("http://foo.com/index.html?this=that&dude=bro");
        assertEquals(kv.parseURLPath("http://foo.com/index.html?this=that&dude=bro", true), "http://foo.com/index.html");
        assertEquals(kv.parseURLPath("&foo=bar", false), "&foo=bar");
        assertEquals(kv.parseURLPath("?foo=bar", true), "");
        assertEquals(kv.parseURLPath("?", true), "");
        assertEquals(kv.parseURLPath("", true), "");

        kv = new KVPairs((String) null);
        kv = new KVPairs(new KVPairs("foo=bar&me=i"));
        assertEquals(kv.toURLParams(), "foo=bar&me=i");

        byte bv[] = kv.toBinArray();
        KVPairs k2 = new KVPairs(bv);
        assertEquals(kv.toString(), k2.toString());
        assertEquals(k2.getPrintable(), "foo='bar' me='i'");
        assertEquals(k2.getPrintable(true), "foo='bar'\nme='i'");

        KVPairs k3 = k2.getCopy();
        assertEquals(k2.toString(), k3.toString());

        KVPairs k4 = new KVPairs("111=222").merge(k3).merge(null);
        assertEquals(k4.toString(), "111=222&foo=bar&me=i");
        k4.fromURLParams("a=b&c=d");
        assertEquals(k4.toString(), "111=222&foo=bar&me=i&a=b&c=d");
    }

    @Test
    public void add() {
        KVPairs kv = new KVPairs();
        kv.replaceOrAdd("int", 1);
        kv.replaceOrAdd("int", 1);
        kv.replaceOrAdd("long", 1L);
        kv.replaceOrAdd("long", 1L);
        kv.replaceOrAdd("jkl", "ooo");
        kv.replaceOrAdd("jkl", "ooo");
        kv.renameValue("jkl", "mno");
        kv.renameValue("jkl", "mno");
        kv.removePair("int");
        kv.removePair("long");
        kv.removePair("mno");
        assertEquals(kv.takeValue("int", 2), 2);
        assertEquals(kv.takeValue("float", 2.0f), 2.0f, 0.01);
        assertEquals(kv.takeValue("def", 2L, 10), 2L);
        assertEquals(kv.takeValue("mno", "2"), "2");
        kv.add("abc", "123");
        kv.add("def", 123);
        kv.addValue("def", 1);
        kv.addValue("def", 1L);
        kv.addValue("def", 1.0f);
        kv.addValue("def", 1.0d);
        kv.add("ghi", null);
        kv.putValue("jkl", 100);
        kv.putValue("jkl", 100L);
        kv.putValue("jkl", 100.0f);
        kv.putValue("jkl", 100.0d);
        kv.putPair(new KVPair("jkl", "iii"));
        assertEquals(kv.getValue("abc"), "123");
        assertEquals(kv.getValue("ABC"), "123");
        assertEquals(kv.getValue("aBc"), "123");
        assertEquals(kv.getValue("def"), "1.0");
        assertEquals(kv.getValue("ghi"), null);
        assertEquals(kv.getValue("foo"), null);
        assertEquals(kv.getValue("foo", "bar"), "bar");
        assertEquals(kv.toString(), "abc=123&def=1.0&ghi=&jkl=iii");
        assertEquals(kv.count(), 4);
    }


    @Test
    public void hasKeyNullSafe() {
        KVPairs kv = new KVPairs();
        kv.add("foo", "bar");
        assertTrue(kv.hasKey("foo"));
        assertFalse(kv.hasKey(null));
    }

}
