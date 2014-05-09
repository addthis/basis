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
public class ReadOnlyAsciiBuf extends AbstractReadOnlyCharBuf {

    public ReadOnlyAsciiBuf(ByteBuf data) {
        super(data);
    }

    public ReadOnlyAsciiBuf(ByteBuf data, boolean validate) {
        super(data, validate);
    }

    public ReadOnlyAsciiBuf(CharBuf charBuf) {
        super(charBuf);
    }

    /**
     * Searches the current underlying data for erroneous values. Useful for testing
     * and untrusted constructors.
     * @return true if valid
     */
    @Override
    protected boolean validate() {
        int indexOf = content().forEachByte(CharBufs.FIND_NEGATIVE);
        return indexOf == -1;
    }

    @Override
    public int length() {
        return content().readableBytes();
    }

    @Override
    public char charAt(int index) {
        return (char) content().getByte(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return new ReadOnlyAsciiBuf(content().slice(start, end));
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

    /**
     * Should be the same hash as that of a String representing the same
     * sequence of characters. This hash code is _not_ cached (for now).
     * It should be trivial to subclass and implement if really needed.
     */
    @Override
    public int hashCode() {
        ByteBuf slice = content().slice();
        int hash = 0;
        if (slice.readableBytes() > 0) {
            for (int i = 0; i < slice.capacity(); i++) {
                hash = 31 * hash + slice.getByte(i);
            }
        }
        return hash;
    }
}
