package com.addthis.basis.chars;

import io.netty.buffer.ByteBuf;

/**
 * A CharSequence backed by a ByteBuf instead of a String. This
 * version supports all characters. The backing store is a
 * variable width utf8 representation, but for char seq's sake,
 * we may emulate their non-sense semi-variable-width chars to
 * some extent when accessed via the CharSequence interface.
 */
public class ReadOnlyUtfBuf extends AbstractReadOnlyCharBuf {

    // thread-safe, lightweight, oppertunistic index cache
    // tracks number of multi-byte chars for a given substring
    // of bytes starting from 0 to a variable end point.
    //
    // it is two shorts packed into one int. The reasoning is
    // that non-volatile int reads/writes are guaranteed to be
    // atomic whereas two shorts (or any length numeral) could
    // sometimes give _wrong_ data instead of just potentially
    // unhelpful data. Longs are not guaranteed to be safe from
    // word tearing so we limit ourselves to working well for up
    // to 65k characters or so. A subclass/ alt impl. may decide
    // to make other trade-offs re: index cache maximums, thread
    // safety etc. if needed.
    // Their non-volatile nature notably benefits us on two fronts.
    // a) It helps single-threaded use by preventing any memory fencing
    // overhead.
    // b) It helps multi-threaded use by making it more likely for
    // changes to not be visible to each other and thereby making it
    // more likely for the cache to remain helpful in that thread's
    // context. The most likely case being two threads concurrently
    // iterating over it via the CharSequence interface.
    //
    // We track the number of multi-byte characters because there
    // can only ever be at most half as many as there are bytes and
    // so we can afford to spent one bit (the sign bit) to optimize
    // more for the ascii-only common case.
    //
    // Finally, it is protected (not private) because here there be
    // dragons and it might be helpful. Likely we will provide a
    // 'final' subclass for maximum safe-string-replacing semantics.
    protected int packedIndexCache;

    public ReadOnlyUtfBuf(ByteBuf data) {
        super(data);
    }

    public ReadOnlyUtfBuf(ByteBuf data, boolean validate) {
        super(data, validate);
    }

    public ReadOnlyUtfBuf(CharBuf charBuf) {
        super(charBuf);
    }

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
        newCache |= (int) byteIndex;
        return newCache;
    }

    protected static short charIndex(short charCount, short byteIndex) {
        return (short) (byteIndex - charCount);
    }

    protected boolean knownAsciiOnly(int cacheInstance) {
        // see if at least one multi-byte character has been found up to the index
        if (cacheInstance < 0) {
            return false;
        }
        // see if the index is the end of the byte array
        return (short) cacheInstance == content().readableBytes();
    }

    @Override
    protected boolean validate() {
        // TODO: ???
        return true;
    }

    @Override
    public int length() {
        int cacheInstance = packedIndexCache;
        if (knownAsciiOnly(cacheInstance)) {
            return content().readableBytes();
        }
        short charCount = cacheCharCount(cacheInstance);
        short byteIndex = cacheByteIndex(cacheInstance);
        for (int i = byteIndex; i < content().readableBytes(); i++) {
            byte b = content().getByte(i);
            if (b < 0) {
                charCount += 1;
            }
        }
        packedIndexCache = packIndexCache(charCount, byteIndex);
        return content().readableBytes() - charCount;
    }

    @Override
    public char charAt(int index) {
        int cacheInstance = packedIndexCache;
//        short byteIndex = cacheByteIndex(cacheInstance);
//        if (index < (int) byteIndex) {
//            if (cacheInstance >= 0) {
//                return (char) content().getByte(index);
//            }
//            int indexDelta = index - byteIndex;
//            if (indexDelta < index) {
//                short charCount = cacheCharCount(cacheInstance);
//                for (int i = byteIndex; i < content().readableBytes(); i++) {
//                    byte b = content().getByte(i);
//                    if (b < 0) {
//                        charCount += 1;
//                    }
//                }
//                packedIndexCache = packIndexCache(charCount, byteIndex);
//            }
//        }
    }

    @Override
    public CharSequence subSequence(int start, int end) {
//        return new ReadOnlyUtfBuf(content().slice(start, end));
    }

    @Override
    public String toString() {
        // Can't find a good way around String's stupid always-copy constructor, but by
        // not using content().toString(UTF8), we can at least prevent one extra allocation.
        //
        // ((CharBuffer alloc, CharBuffer toString, new String) -> (char[] alloc, new String))
        //
        // If desperate, _might_ be able to hack it with a dummy CharacterEncoder if there is
        // no security manager. Otherwise have to class path boot etc to get into the lang
        // package. I suppose annoyances like these are why I made this package.
//        ByteBuf slice = content().slice();
//        char[] values = new char[slice.capacity()];
//        if (slice.readableBytes() > 0) {
//            for (int i = 0; i < slice.capacity(); i++) {
//                values[i] = (char) slice.getByte(i);
//            }
//        } else {
//            return "";
//        }
//        return new String(values);
    }

    /**
     * Should be the same hash as that of a String representing the same
     * sequence of characters. This hash code is _not_ cached (for now).
     * It should be trivial to subclass and implement if really needed.
     */
    @Override
    public int hashCode() {
//        ByteBuf slice = content().slice();
//        int hash = 0;
//        if (slice.readableBytes() > 0) {
//            for (int i = 0; i < slice.capacity(); i++) {
//                hash = 31 * hash + slice.getByte(i);
//            }
//        }
//        return hash;
    }
}
