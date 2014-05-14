package com.addthis.basis.chars;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public abstract class CharSequenceTests {


    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        CharSequence oneByteUtfControl   = "hello, world";
        CharSequence twoByteUtfControl   = "hello, " + (char) 372  + (char) 332 + "rl" + (char) 272 + "!";
        CharSequence threeByteUtfControl = "hello, " + (char) 2050 + (char) 4095 + "rl" + (char) 65520 + "!";
        CharSequence fourByteUtfControl  = "hello, " + new String(Character.toChars(70000)) + " oh god wh" +
                                           new String(Character.toChars(75000));

        return Arrays.asList(new Object[][]{
                {oneByteUtfControl}, {twoByteUtfControl}, {threeByteUtfControl}, {fourByteUtfControl}
        });
    }

    private final CharSequence controlSequence;

    public CharSequenceTests(CharSequence controlSequence) {
        this.controlSequence = controlSequence;
    }

    public abstract CharSequence getCharSequenceForString(CharSequence control);

    @Test
    public void sameLength() {
        CharSequence charBuf = getCharSequenceForString(controlSequence);
        assertEquals((long) controlSequence.length(),  (long) charBuf.length());
    }

    @Test
    public void fullOneTimeIteration() {
        CharSequence charBuf = getCharSequenceForString(controlSequence);
        for (int i = 0; i < controlSequence.length(); i++) {
            assertEquals("index " + i, (long) controlSequence.charAt(i), (long) charBuf.charAt(i));
        }
    }

    @Test
    public void subsequenceOneTimeIteration() {
        CharSequence sample = controlSequence;
        CharSequence charBuf = getCharSequenceForString(sample);

        charBuf = charBuf.subSequence(1,3);
        sample = sample.subSequence(1,3);
        for (int i = 0; i < sample.length(); i++) {
            assertEquals((long) sample.charAt(i), (long) charBuf.charAt(i));
        }
    }

    @Test
    public void indexWithoutIteration() {
        CharSequence charBuf = getCharSequenceForString(controlSequence);
        assertEquals((long) controlSequence.charAt(4), (long) charBuf.charAt(4));
        assertEquals((long) controlSequence.length(),  (long) charBuf.length());
    }

    @Test
    public void severalNoncontiguousDecreasingIndexes() {
        CharSequence charBuf = getCharSequenceForString(controlSequence);
        assertEquals((long) controlSequence.charAt(4), (long) charBuf.charAt(4));
        assertEquals((long) controlSequence.charAt(2), (long) charBuf.charAt(2));
        assertEquals((long) controlSequence.charAt(1), (long) charBuf.charAt(1));
        assertEquals((long) controlSequence.length(),  (long) charBuf.length());
    }
}
