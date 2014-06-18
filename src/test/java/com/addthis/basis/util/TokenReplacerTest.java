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
import static org.junit.Assert.fail;

public class TokenReplacerTest {

    @Test
    public void testReplacer() {
        TokenReplacer finite = new TokenReplacer("%{", "}%") {
            @Override
            public String replace(Region region, String src) {
                return "a";
            }

            @Override
            public long getMaxDepth() {
                return 10;
            }
        };
        try {
            assertEquals("a", finite.process("%{foobar}%"));
        } catch (TokenReplacerOverflowException ex) {
            fail();
        }
    }

    @Test
    public void testMaxDepth() {
        TokenReplacer infinite = new TokenReplacer("%{", "}%") {
            @Override
            public String replace(Region region, String src) {
                return "%{" + "a" + src + "}%";
            }

            @Override
            public long getMaxDepth() {
                return 10;
            }
        };
        boolean fail = false;
        try {
            infinite.process("%{}%");
        } catch (TokenReplacerOverflowException ex) {
            assertEquals("Depth 10 exceeded trying to expand \"%{}%\"", ex.getMessage());
            fail = true;
        }
        assertTrue(fail);
    }

}
