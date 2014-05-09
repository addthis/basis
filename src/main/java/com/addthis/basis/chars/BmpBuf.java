package com.addthis.basis.chars;

import java.io.IOException;

import java.nio.charset.UnmappableCharacterException;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.DefaultByteBufHolder;

/**
 * A CharSequence backed by a ByteBuf instead of a String. This
 * version supports characters in the Basic Multilingual Plane (BMP).
 * That includes all ASCII characters (UTF-8 w/ 1 byte) and most (all?)
 * java char type characters (UTF-8 w/ 2 bytes). 3-4 byte characters
 * (which java calls supplementary characters) will be supported if and
 * when analysis shows them being used in significant amounts in practice.
 * If nothing else though, we should include the faculty to fall back to
 * java Strings or some such for supplementary characters.
 *
 * The ideal would be to gracefully degrade in the presence of increasing
 * numbers of 2-byte characters not unlike UTF-8 encoding itself. It is
 * not necessarily obvious the best way to go about that though. There are
 * several different ways to do so off the top of my head -- each with
 * various trade offs under different use cases. It is probable that this
 * version may benefit from several versions and/ or an intelligent, self-
 * adapting catch-all.
 *
 * Some possible degredation options:
 *
 * Keep Multi-byte Characters in the ByteBuf in line with 1-byters
 *      pros: can still read/ write straight from serialized form without
 *      any additional processing. Most memory efficient wrt/ storage of
 *      characters.
 *
 *      cons: length and indexing are no longer consistent with byte indexing.
 *      Unless mitigated somehow, this would cause O(n) length and index ops.
 *      Mitigation strategies are a plenty, but numerous and with their own
 *      vying trade-offs.
 *
 * Switch to All Double-Byte Characters
 *      pros: no complicated mitigation techniques to worry about. (ie.
 *      consistent, constant time indexing/ length)
 *      Easy with java char primitives and bytebuf get/put Char methods.
 *
 *      cons: would have to somehow intervene or otherwise signal that the
 *      content() can not be written as is due to the extra null bytes.
 *      uses up to nearly double the amount of required memory.
 *
 * Store Secondary Bytes in an adaptable extra data structure
 *      pros: keep consistent, constant time index/length.
 *      more memory efficient than all double-byte chars, and easy to degrade.
 *      makes use of the built-in flag for efficient handling of single-byters
 *      and other nice degradation options.
 *
 *      cons: still have to intervene in some way as with 'all double-byte' characters.
 *      almost certainly some point where after the number of double-byters
 *      is > some value and/ or percentage where it is just worse than 'all double-byte'
 *
 */
public abstract class BmpBuf extends DefaultByteBufHolder implements CharBuf {

    public BmpBuf(ByteBuf data) {
        super(data);
        if (!validate()) {
            throw new RuntimeException(new UnmappableCharacterException(2));
        }
    }

    /**
     * Searches the current underlying data for erroneous values. Useful for testing
     * and untrusted constructors.
     * @return true if valid
     */
    abstract boolean validate();

    @Override
    public Appendable append(CharSequence csq) throws IOException {
        return append(csq, 0, csq.length());
    }

    @Override
    public abstract Appendable append(CharSequence csq, int start, int end) throws IOException;

    @Override
    public abstract Appendable append(char c) throws IOException;

    @Override
    public abstract int length();

    @Override
    public abstract char charAt(int index);

    @Override
    public abstract CharSequence subSequence(int start, int end);

    @Override
    public abstract String toString();

    /**
     * Should be the same hash as that of a String representing the same
     * sequence of characters. This hash code is _not_ cached (for now).
     * It should be trivial to subclass and implement if really needed.
     */
    @Override
    public abstract int hashCode();

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof CharBuf) {
            return ByteBufUtil.equals(((CharBuf) obj).content(), content());
        }
        return false;
    }

    @Override
    public int compareTo(ReadableCharBuf o) {
        return ByteBufUtil.compare(o.content(), content());
    }
}
