package com.addthis.basis.chars;

import org.junit.Assert;
import org.junit.Test;

import static com.addthis.basis.chars.ReadOnlyUtfBuf.cacheByteIndex;
import static com.addthis.basis.chars.ReadOnlyUtfBuf.cacheCharCount;
import static com.addthis.basis.chars.ReadOnlyUtfBuf.packIndexCache;

public class ReadOnlyUtfBufTest {

    private static int packAndCheck(int chars, int bytes) {
        int cache = packIndexCache((short) chars, (short) bytes);
        Assert.assertEquals(chars, cacheCharCount(cache));
        Assert.assertEquals(bytes, cacheByteIndex(cache));
        return cache;
    }

    @Test
    public void cachePersist() {
        packAndCheck(12, 54);
        packAndCheck(0, 0);
        packAndCheck(1, 0);
        packAndCheck(0, 1);
        packAndCheck(2, 1);
    }

    @Test
    public void negativeShortcut() {
        Assert.assertTrue(packAndCheck(12, 12) < 0);
        Assert.assertTrue(packAndCheck(2, 0) < 0);
        Assert.assertTrue(packAndCheck(2, 1) < 0);
        Assert.assertTrue(packAndCheck(0, 12) >= 0);
        Assert.assertTrue(packAndCheck(0, 0) >= 0);
        Assert.assertTrue(packAndCheck(0, 999) >= 0);
    }

}