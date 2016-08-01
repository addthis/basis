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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.addthis.basis.util.CookieSafeBase90.decodeBase45;
import static com.addthis.basis.util.CookieSafeBase90.decodeBase90;
import static com.addthis.basis.util.CookieSafeBase90.decodeBytesBase90;
import static com.addthis.basis.util.CookieSafeBase90.encodeBase45;
import static com.addthis.basis.util.CookieSafeBase90.encodeBase90Signed;
import static com.addthis.basis.util.CookieSafeBase90.encodeBase90Unsigned;
import static com.addthis.basis.util.CookieSafeBase90.encodeBytesBase90;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CookieSafeBase90Test {
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void shouldEncodeBase45Correctly() {
        assertEquals("#", encodeBase45(1L, false, false));
        assertEquals("$", encodeBase45(2L, false, false));
        assertEquals("R", encodeBase45(1L, true, false));
        assertEquals("S", encodeBase45(2L, true, false));
        assertEquals("P", encodeBase45(44L, false, false));
        assertEquals("~", encodeBase45(44L, true, false));
        assertEquals("#!", encodeBase45(45L, false, false));
        assertEquals("RQ", encodeBase45(45L, true, false));
        assertEquals("!R", encodeBase45(1L, false, true));
        assertEquals("!S", encodeBase45(2L, false, true));
        assertEquals("Q#", encodeBase45(1L, true, true));
        assertEquals("Q$", encodeBase45(2L, true, true));
        assertEquals("!~", encodeBase45(44L, false, true));
        assertEquals("QP", encodeBase45(44L, true, true));
        assertEquals("#Q", encodeBase45(45L, false, true));
        assertEquals("R!", encodeBase45(45L, true, true));
        assertEquals("RQ!", encodeBase45(2025L, true, true));
        assertEquals("RQQ", encodeBase45(2025L, true, false));
        assertEquals("#!Q", encodeBase45(2025L, false, true));
        assertEquals("#!!", encodeBase45(2025L, false, false));
        assertEquals("~P", encodeBase45(2024L, true, true));
        assertEquals("~~", encodeBase45(2024L, true, false));
        assertEquals("P~", encodeBase45(2024L, false, true));
        assertEquals("PP", encodeBase45(2024L, false, false));
    }

    @Test
    public void encodeAndDecodeMaxValueBase45() {
        String encoded = CookieSafeBase90.encodeBase45(Long.MAX_VALUE, true, false);
        long decoded = decodeBase45(encoded);
        assertEquals(Long.MAX_VALUE, decoded);
    }

    @Test
    public void encodeAndDecodeMaxValueBase90() {
        String encoded = encodeBase90Signed(Long.MAX_VALUE);
        long decoded = decodeBase90(encoded, true);
        assertEquals(Long.MAX_VALUE, decoded);
    }

    @Test
    public void shouldDecodeBase45Correctly() {
        assertEquals(1, decodeBase45("#"));
        assertEquals(2, decodeBase45("$"));
        assertEquals(1, decodeBase45("R"));
        assertEquals(2, decodeBase45("S"));
        assertEquals(44, decodeBase45("P"));
        assertEquals(44, decodeBase45("~"));
        assertEquals(45, decodeBase45("#!"));
        assertEquals(45, decodeBase45("RQ"));
        assertEquals(1, decodeBase45("!R"));
        assertEquals(2, decodeBase45("!S"));
        assertEquals(1, decodeBase45("Q#"));
        assertEquals(2, decodeBase45("Q$"));
        assertEquals(44, decodeBase45("!~"));
        assertEquals(44, decodeBase45("QP"));
        assertEquals(45, decodeBase45("#Q"));
        assertEquals(45, decodeBase45("R!"));
        assertEquals(2025, decodeBase45("RQ!"));
        assertEquals(2025, decodeBase45("RQQ"));
        assertEquals(2025, decodeBase45("#!Q"));
        assertEquals(2025, decodeBase45("#!!"));
        assertEquals(2024, decodeBase45("~P"));
        assertEquals(2024, decodeBase45("~~"));
        assertEquals(2024, decodeBase45("P~"));
        assertEquals(2024, decodeBase45("PP"));
        assertEquals(12345, decodeBase45("WU2"));
    }

    @Test
    public void exceptionOnBadChar() {
        thrown.expect(RuntimeException.class);
        decodeBase45(";");
    }

    @Test
    public void exceptionOnBadChar2() {
        thrown.expect(RuntimeException.class);
        decodeBase45(";");
    }

    @Test
    public void exceptionOnBadChar3() {
        thrown.expect(RuntimeException.class);
        decodeBase45("abcdef;");
    }

    @Test
    public void shouldEncodeBase90UnsignedCorrectly() {
        assertEquals("#!", encodeBase90Unsigned(90));
        assertEquals("~", encodeBase90Unsigned(89));
        assertEquals("!", encodeBase90Unsigned(0));
        assertEquals("#", encodeBase90Unsigned(1));
        assertEquals("#!!", encodeBase90Unsigned(8100));
        assertEquals("~~", encodeBase90Unsigned(8099));
        assertEquals("#S2", encodeBase90Unsigned(12345));
    }

    @Test
    public void shouldEncodeBase90SignedCorrectly() {
        assertEquals("Q!!!!!!!!~", encodeBase90Signed(-89));
        assertEquals("Q!!!!!!!~~", encodeBase90Signed(-8099));
        assertEquals("Q!!!!!!#S2", encodeBase90Signed(-12345));
        assertEquals("im^ldBVL7)", encodeBase90Signed(-Long.MAX_VALUE));
        assertEquals("!!!!!!!!!~", encodeBase90Signed(89));
        assertEquals("!!!!!!!!~~", encodeBase90Signed(8099));
        assertEquals("!!!!!!!#S2", encodeBase90Signed(12345));
        assertEquals(":m^ldBVL7)", encodeBase90Signed(Long.MAX_VALUE));
        assertEquals("!!!!!!!!!!", encodeBase90Signed(0));
    }

    @Test
    public void shouldDecodeBase90SignedCorrectly() {
        assertEquals(-89, decodeBase90("Q!!!!!!!!~", true));
        assertEquals(-8099, decodeBase90("Q!!!!!!!~~", true));
        assertEquals(-12345, decodeBase90("Q!!!!!!#S2", true));
        assertEquals(-Long.MAX_VALUE, decodeBase90("im^ldBVL7)", true));
        assertEquals(89, decodeBase90("!!!!!!!!!~", true));
        assertEquals(8099, decodeBase90("!!!!!!!!~~", true));
        assertEquals(12345, decodeBase90("!!!!!!!#S2", true));
        assertEquals(Long.MAX_VALUE, decodeBase90(":m^ldBVL7)", true));
        assertEquals(0, decodeBase90("!!!!!!!!!!", true));
    }

    @Test
    public void shouldDecodeBase90UnsignedCorrectly() {
        assertEquals(90, decodeBase90("#!", false));
        assertEquals(89, decodeBase90("~", false));
        assertEquals(0, decodeBase90("!", false));
        assertEquals(1, decodeBase90("#", false));
        assertEquals(8100, decodeBase90("#!!", false));
        assertEquals(8099, decodeBase90("~~", false));
        assertEquals(12345, decodeBase90("#S2", false));
    }

    @Test
    public void shouldEncodeBytes() {
        assertEquals("6ISXAu6*VX'", CookieSafeBase90.encodeBytesBase90("hello".getBytes()));
        assertEquals("hello", new String(decodeBytesBase90("6ISXAu6*VX'")));
        assertTrue(stringEncodesAndDecodesToItself("x˚xkrf˚"));
        assertTrue(stringEncodesAndDecodesToItself("hello"));
        assertTrue(stringEncodesAndDecodesToItself("काचं शक्नोम्यत्तुम्"));
        assertTrue(stringEncodesAndDecodesToItself("Μπορῶ νὰ φάω σπασμένα γυαλιὰ χωρὶς νὰ πάθω τίποτα."));
        assertTrue(stringEncodesAndDecodesToItself("ᚅᚔᚉᚉ"));
        assertTrue(stringEncodesAndDecodesToItself("ᚷᛚᚨᛋ᛫ᛖᚩᛏᚪᚾ᛫ᚩᚾᛞ᛫ᚻᛁᛏ᛫ᚾᛖ᛫ᚻᛖᚪᚱᛗᛁᚪᚧ᛫ᛗᛖ᛬"));
        assertTrue(stringEncodesAndDecodesToItself("నేను గాజు తినగలను మరియు"));
        assertTrue(stringEncodesAndDecodesToItself("ฉันกินกระจกได้ แต่มันไม่ทำให้ฉันเจ็บ"));
        assertTrue(stringEncodesAndDecodesToItself("我能吞下玻璃而不傷身體"));
        assertTrue(stringEncodesAndDecodesToItself("أنا قادر على أكل الزجاج و هذا لا يؤلمني."));
        assertTrue(stringEncodesAndDecodesToItself("אני יכול לאכול זכוכית וזה לא מזיק לי."));
        assertTrue(stringEncodesAndDecodesToItself("איך קען עסן גלאָז און עס טוט מיר נישט װײ."));
        assertTrue(stringEncodesAndDecodesToItself("色は匂へど 散りぬるを"));
    }

    private boolean stringEncodesAndDecodesToItself(String s) {
        String encoded = encodeBytesBase90(s.getBytes());
        String decoded = new String(decodeBytesBase90(encoded));
        return decoded.equals(s);
    }
}
