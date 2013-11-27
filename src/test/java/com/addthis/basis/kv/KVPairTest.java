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
import static org.junit.Assert.assertTrue;

public class KVPairTest {
    @Test
    public void parseHttpHeader() {
        testPair(KVPair.parseHttpHeader("abc:def"), "abc", "def");
        testPair(KVPair.parseHttpHeader("abc: def"), "abc", "def");
        testPair(KVPair.parseHttpHeader("abc:  def"), "abc", "def");
        testPair(KVPair.parseHttpHeader("abc:   def"), "abc", "def");
        testPair(KVPair.parseHttpHeader("abc:   "), "abc", " ");
        testPair(KVPair.parseHttpHeader("abc: "), "abc", " ");
        testPair(KVPair.parseHttpHeader("abc:"), "abc", "");
        testPair(KVPair.parseHttpHeader("abc"), "abc", "");
        assertEquals(KVPair.parseHttpHeader(":"), null);
    }

    @Test
    public void parsePair() {
        testPair(KVPair.parsePair("abc=def"), "abc", "def");
        testPair(KVPair.parsePair("abc==x"), "abc", "=x");
        testPair(KVPair.parsePair("abc=="), "abc", "=");
        testPair(KVPair.parsePair("abc= "), "abc", " ");
        testPair(KVPair.parsePair("abc="), "abc", "");
        testPair(KVPair.parsePair("abc"), "abc", "");
        assertEquals(KVPair.parsePair("=123"), null);
        assertEquals(KVPair.parsePair("="), null);
        assertEquals(KVPair.parsePair(""), null);
    }

    @Test
    public void encoding() {
        assertTrue(new KVPair("abc", "def").toString().equals("abc=def"));
        assertTrue(new KVPair("abc", "def").keyMatch("abc"));
    }

    private void testPair(KVPair kp, String key, String value) {
        assertTrue(key != null);
        assertTrue(value != null);
        assertEquals(kp.getKey().toUpperCase(), key.toUpperCase());
        assertEquals(kp.getValue(), value);
    }
}
