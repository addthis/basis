package com.addthis.basis.chars;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import static com.addthis.basis.chars.ReadOnlyUtfBuf.cacheByteIndex;
import static com.addthis.basis.chars.ReadOnlyUtfBuf.cacheCharDelta;
import static com.addthis.basis.chars.ReadOnlyUtfBuf.packIndexCache;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class ReadOnlyUtfBufTest {

    private static int packAndCheck(int chars, int bytes) {
        int cache = packIndexCache((short) chars, (short) bytes);
        Assert.assertEquals(chars, cacheCharDelta(cache));
        Assert.assertEquals(bytes, cacheByteIndex(cache));
        return cache;
    }

    @Test
    public void simpleAscii() {
        String sample = "hello, world";
        byte[] bytes = sample.getBytes(StandardCharsets.UTF_8);
        ByteBuf byteBuf = Unpooled.wrappedBuffer(bytes);
        ReadOnlyUtfBuf charBuf = new ReadOnlyUtfBuf(byteBuf);
        for (int i = 0; i < sample.length(); i++) {
            Assert.assertEquals(String.valueOf(sample.charAt(i)), String.valueOf(charBuf.charAt(i)));
        }
    }

    @Test
    public void simpleAsciiSubSequence() {
        String sample = "hello, world";
        byte[] bytes = sample.getBytes(StandardCharsets.UTF_8);
        ByteBuf byteBuf = Unpooled.wrappedBuffer(bytes);
        ReadOnlyUtfBuf charBuf = new ReadOnlyUtfBuf(byteBuf);
        charBuf = (ReadOnlyUtfBuf) charBuf.subSequence(1,3);
        sample = (String) sample.subSequence(1,3);
        for (int i = 0; i < sample.length(); i++) {
            Assert.assertEquals(String.valueOf(sample.charAt(i)), String.valueOf(charBuf.charAt(i)));
        }
    }

    @Test
    public void twoByteUtfSingleIndex() {
        String sample = "hello, ";
        sample += (char) 372;
        sample += (char) 332;
        sample += "rl";
        sample += (char) 272;
        sample += "!";
//        System.out.println(sample);
        byte[] bytes = sample.getBytes(StandardCharsets.UTF_8);
        ByteBuf byteBuf = Unpooled.wrappedBuffer(bytes);
        ReadOnlyUtfBuf charBuf = new ReadOnlyUtfBuf(byteBuf);
        Assert.assertEquals(String.valueOf(sample.charAt(4)), String.valueOf(charBuf.charAt(4)));
        Assert.assertEquals(sample.length(), charBuf.length());
    }

    @Test
    public void twoByteUtfSeveralIndexes() {
        String sample = "hello, ";
        sample += (char) 372;
        sample += (char) 332;
        sample += "rl";
        sample += (char) 272;
        sample += "!";
//        System.out.println(sample);
        byte[] bytes = sample.getBytes(StandardCharsets.UTF_8);
        ByteBuf byteBuf = Unpooled.wrappedBuffer(bytes);
        ReadOnlyUtfBuf charBuf = new ReadOnlyUtfBuf(byteBuf);
        Assert.assertEquals(String.valueOf(sample.charAt(4)), String.valueOf(charBuf.charAt(4)));
        Assert.assertEquals(String.valueOf(sample.charAt(2)), String.valueOf(charBuf.charAt(2)));
        Assert.assertEquals(String.valueOf(sample.charAt(1)), String.valueOf(charBuf.charAt(1)));
        Assert.assertEquals(sample.length(), charBuf.length());
    }

    @Test
    public void twoByteUtfSubSequence() {
        String sample = "hello, ";
        sample += (char) 372;
        sample += (char) 332;
        sample += "rl";
        sample += (char) 272;
        sample += "!";
//        System.out.println(sample);
        byte[] bytes = sample.getBytes(StandardCharsets.UTF_8);
        ByteBuf byteBuf = Unpooled.wrappedBuffer(bytes);
        ReadOnlyUtfBuf charBuf = new ReadOnlyUtfBuf(byteBuf);
        charBuf = (ReadOnlyUtfBuf) charBuf.subSequence(1,3);
        sample = (String) sample.subSequence(1,3);
        for (int i = 0; i < sample.length(); i++) {
            Assert.assertEquals(String.valueOf(sample.charAt(i)), String.valueOf(charBuf.charAt(i)));
        }
        Assert.assertEquals(sample.length(), charBuf.length());
    }

    @Test
    public void twoByteUtf() {
        String sample = "hello, ";
        sample += (char) 372;
        sample += (char) 332;
        sample += "rl";
        sample += (char) 272;
        sample += "!";
//        System.out.println(sample);
        byte[] bytes = sample.getBytes(StandardCharsets.UTF_8);
        ByteBuf byteBuf = Unpooled.wrappedBuffer(bytes);
        ReadOnlyUtfBuf charBuf = new ReadOnlyUtfBuf(byteBuf);
        for (int i = 0; i < sample.length(); i++) {
            Assert.assertEquals(String.valueOf(sample.charAt(i)), String.valueOf(charBuf.charAt(i)));
        }
        Assert.assertEquals(sample.length(), charBuf.length());
    }

    @Test
    public void threeByteUtf() {
        String sample = "hello, ";
        sample += (char) 2050;
        sample += (char) 4095;
        sample += "rl";
        sample += (char) 65520;
        sample += "!";
//        System.out.println(sample);
        byte[] bytes = sample.getBytes(StandardCharsets.UTF_8);
        ByteBuf byteBuf = Unpooled.wrappedBuffer(bytes);
        ReadOnlyUtfBuf charBuf = new ReadOnlyUtfBuf(byteBuf);
        for (int i = 0; i < sample.length(); i++) {
            Assert.assertEquals(sample.charAt(i), charBuf.charAt(i));
        }
        Assert.assertEquals(sample.length(), charBuf.length());
    }

    @Test
    public void cachePersist() {
        packAndCheck(-12, 54);
        packAndCheck(0, 0);
        packAndCheck(-1, 0);
        packAndCheck(0, 1);
        packAndCheck(-2, 1);
    }

    @Test
    public void negativeShortcut() {
        Assert.assertTrue(packAndCheck(-12, 12) < 0);
        Assert.assertTrue(packAndCheck(-2, 0) < 0);
        Assert.assertTrue(packAndCheck(-2, 1) < 0);
        Assert.assertTrue(packAndCheck(0, 12) >= 0);
        Assert.assertTrue(packAndCheck(0, 0) >= 0);
        Assert.assertTrue(packAndCheck(0, 999) >= 0);
    }

    private static class SharedCache {

        protected int packedIndexCache;

        // upper half of packedIndexCache; must use a locally stored copy of the cache value to be thread safe
        // it is packed as a negative value counting down from zero. this lets us do lazier ascii purity queries
        // nb. it is okay to invert before shifting, but not okay to add due to packed short interactions
        protected static short cacheCharCount(int cacheInstance) {
            return (short) ((~cacheInstance >> 16) + 1);
        }

        // lower half of packedIndexCache; must use a locally stored copy of the cache value to be thread safe
        protected static short cacheByteIndex(int cacheInstance) {
            return (short) cacheInstance;
        }

        protected static int packIndexCache(short charCount, short byteIndex) {
            int newCache = ~((int) charCount) + 1;
            newCache <<= 16;
            newCache |= byteIndex;
            return newCache;
        }
    }

    // Ignored because it doesn't really test anything that can be failed, it is just
    // here to enable testing of the concurrent behavior of the packed cache if desired.
    @Test @Ignore
    public void cacheSharing() throws Throwable {
        final int iterations = 30000;
        byte[] bytey = new byte[iterations];
        for (int i = 0; i < iterations; i++) {
            bytey[i] = (byte) (Math.random() * 255);
        }
        ExecutorService executor = Executors.newCachedThreadPool();
        cacheSharing0(executor, bytey);
        System.out.println("  ");
        cacheSharing0(executor, bytey);
        System.out.println("  ");
        cacheSharing0(executor, bytey);
        executor.shutdown();
        boolean result = executor.awaitTermination(30, TimeUnit.SECONDS);
        Assert.assertTrue("executor did not shut down in 30 seconds", result);
    }

    public static void cacheSharing0(ExecutorService executor, final byte[] bytey) throws Throwable {
        final SharedCache sharedCache = new SharedCache();
        Future<Long>[] futures = new Future[10];
        for (int i = 0; i < futures.length; i++) {
            futures[i] = executor.submit(new Callable<Long>() {
                public Long call() throws Exception {
                    int deltas = 0;
                    int totalDeltas = 0;
                    for (int j = 0; j < bytey.length; j++) {
                        int cacheInstance = sharedCache.packedIndexCache;
                        short byteIndex = SharedCache.cacheByteIndex(cacheInstance);
                        short charCount = SharedCache.cacheCharCount(cacheInstance);
                        int delta = j - byteIndex;
                        if (delta == 0) {
//                            totalDeltas += delta;
                            sharedCache.packedIndexCache = packAndCheck((int) charCount, j + 1);
                        } else if (delta > 0) {
                            deltas += 1;
                            totalDeltas += delta;
                            if (bytey[j] < 0) {
                                charCount += 1;
                            }
                            sharedCache.packedIndexCache = packAndCheck((int) charCount, j + 1);
                        } else {
                            deltas += 1;
                            delta = -delta;
                            if (delta > j) {
                                delta = j;
                            }
                            totalDeltas += delta;
                            if (bytey[j] < 0) {
                                charCount += 1;
                            }
                            sharedCache.packedIndexCache = packAndCheck((int) charCount, j + 1);
                        }
                    }

                    long out = deltas;
                    out <<= 32;
                    out |= totalDeltas;
                    return out;
                }
            });
        }

        try {
            for (Future<Long> future : futures) {
                long pack = future.get();
                int deltas = (int) (pack >> 32);
                int total  = (int) pack;
                double avg = (double) total / deltas;
                System.out.println("deltas " + deltas + "  totalDelta  " + total + "  avg  " + avg);
            }
        } catch (ExecutionException ex) {
            throw ex.getCause();
        }

    }

}