package com.addthis.basis.chars;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public abstract class CharSequenceTests {

    public static CharSequence oneByteUtfControl   = "hello, world";
    public static CharSequence twoByteUtfControl   = "hello, " + (char) 372  + (char) 332 + "rl" + (char) 272 + "!";
    public static CharSequence threeByteUtfControl = "hello, " + (char) 2050 + (char) 4095 + "rl" + (char) 65520 + "!";

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {oneByteUtfControl}, {twoByteUtfControl}, {threeByteUtfControl}
        });
    }

    private final CharSequence controlSequence;

    public CharSequenceTests(CharSequence controlSequence) {
        this.controlSequence = controlSequence;
    }

    public abstract CharSequence getCharSequenceForString(CharSequence control);

    @Test
    public void fullOneTimeIteration() {
        CharSequence charBuf = getCharSequenceForString(controlSequence);
        for (int i = 0; i < controlSequence.length(); i++) {
            assertEquals(String.valueOf(controlSequence.charAt(i)), String.valueOf(charBuf.charAt(i)));
        }
    }

    @Test
    public void subsequenceOneTimeIteration() {
        CharSequence sample = controlSequence;
        CharSequence charBuf = getCharSequenceForString(sample);

        charBuf = charBuf.subSequence(1,3);
        sample = sample.subSequence(1,3);
        for (int i = 0; i < sample.length(); i++) {
            assertEquals(String.valueOf(sample.charAt(i)), String.valueOf(charBuf.charAt(i)));
        }
    }

    @Test
    public void indexWithoutIteration() {
        CharSequence charBuf = getCharSequenceForString(controlSequence);
        assertEquals(controlSequence.length(), charBuf.length());
        assertEquals(String.valueOf(controlSequence.charAt(4)), String.valueOf(charBuf.charAt(4)));
        assertEquals(controlSequence.length(), charBuf.length());
    }

    @Test
    public void severalNoncontiguousDecreasingIndexes() {
        CharSequence charBuf = getCharSequenceForString(controlSequence);
        assertEquals(String.valueOf(controlSequence.charAt(4)), String.valueOf(charBuf.charAt(4)));
        assertEquals(String.valueOf(controlSequence.charAt(2)), String.valueOf(charBuf.charAt(2)));
        assertEquals(String.valueOf(controlSequence.charAt(1)), String.valueOf(charBuf.charAt(1)));
        assertEquals(controlSequence.length(), charBuf.length());
    }
}
