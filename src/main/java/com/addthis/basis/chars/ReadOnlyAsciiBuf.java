package com.addthis.basis.chars;

import io.netty.buffer.ByteBuf;

/**
 * A CharSequence backed by a ByteBuf instead of a String. This
 * version only supports ASCII characters and so while it supports
 * mutation operations, it is best used for immutable Strings that
 * have a known, fixed Charset satisfied by the range of ASCII
 * characters. (Note: it is called Ascii to make it easier to quickly
 * understand, but technically it allows all single-byte UTF-8 chars;
 * including the NULL value 0).
 *
 * This should be particularly helpful for Strings that are
 * quickly or frequently serialized or deserialized and may not
 * even be looked.
 *
 * It uses a backing ByteBuf, so it extends DefaultByteBufHolder,
 * and should be released when no longer being used to ensure the
 * proper release of resources.
 *
 * 7/8 bit efficient note: Since in this case we are restricting the domain
 * of characters to single-bytes, we could theoretically represent them using
 * 1/8th less space. However, for a large number of reasons this would be
 * inconvenient and have other unknown performance properties. Therefore it
 * is left as a future excercise to verify and support that use case.
 *
 * malformed character handling: It might be reasonable to have an option or
 * an implementation, etc, that handles non-ascii characters in a non-
 * exceptional way. The JDK NIO Character encoding jazz has a bit of support
 * for this already. Two most likely use cases: a) transforming accented latin
 * characters into their plain ascii counterparts and b) replacing with '?' or
 * ' ' and the like.
 */
public class ReadOnlyAsciiBuf extends ReadOnlyUtfBuf {

    public ReadOnlyAsciiBuf(ByteBuf data) {
        super(data);
    }

    public ReadOnlyAsciiBuf(CharBuf charBuf) {
        super(charBuf);
    }

    @Override
    protected boolean knownAsciiOnly(int cacheInstance) {
        return true;
    }

    @Override
    public int length() {
        return _getByteLength();
    }

    @Override
    public char charAt(int index) {
        return (char) _getByte(index);
    }

    // start is inclusive, end is exclusive
    @Override
    public CharSequence subSequence(int start, int end) {
        return _getSubSequenceForByteBounds(start, end);
    }

    /**
     * Uses the cacheInstance for the hashCache (cacheHash? whichever dumb name Donnelly wanted)
     */
    @Override
    public int hashCode() {
        int hash = packedIndexCache;
        if (hash == 0) {
            int length = length();
            for (int i = 0; i < length; i++) {
                char c = charAt(i);
                hash = 31 * hash + c;
            }
            packedIndexCache = hash;
        }
        return hash;
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
        ByteBuf slice = content().slice();
        char[] values = new char[slice.capacity()];
        if (slice.readableBytes() > 0) {
            for (int i = 0; i < slice.capacity(); i++) {
                values[i] = (char) slice.getByte(i);
            }
        } else {
            return "";
        }
        return new String(values);
    }
}
