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
