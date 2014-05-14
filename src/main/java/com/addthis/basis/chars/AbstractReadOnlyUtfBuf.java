package com.addthis.basis.chars;

import com.google.common.base.Objects;

/**
 * A CharSequence backed by utf-8 bytes instead of java chars (ie. utf-16 bytes)
 */
public abstract class AbstractReadOnlyUtfBuf implements CharSequence {

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
    // so we can afford to spend one bit (the sign bit) to optimize
    // more for the ascii-only common case.
    //
    // Finally, it is protected (not private) because here there be
    // dragons and it might be helpful. Likely we will provide a
    // 'final' subclass for maximum safe-string-replacing semantics.
    protected int packedIndexCache;

    protected static final int MAX_USHORT = 65535;

    // upper half of packedIndexCache; must use a locally stored copy of the cache value to be thread safe
    // it is a negative value counting down from zero. this lets us do lazier ascii purity queries/ adds
    // nb. it is okay to invert before shifting, but not okay to add due to packed short interactions
    protected static short cacheCharDelta(int cacheInstance) {
            return (short) (cacheInstance >> 16);
    }

    // lower half of packedIndexCache; must use a locally stored copy of the cache value to be thread safe
    protected static int cacheByteIndex(int cacheInstance) {
        return 0xFFFF & cacheInstance;
    }

    protected static int packIndexCache(short charDelta, int byteIndex) {
        int newCache = (int) charDelta;
        newCache <<= 16;
        newCache |= byteIndex;
        return newCache;
    }

    protected static int charIndex(short charDelta, int byteIndex) {
        return byteIndex + charDelta;
    }

    protected boolean knownAsciiOnly(int cacheInstance) {
        // see if at least one multi-byte character has been found up to the index
        if (cacheInstance < 0) {
            return false;
        }
        // see if the index is the end of the byte array
        return (0xFFFF & cacheInstance) == _getByteLength();
    }

    // get arbitrary byte from backing byte store
    protected abstract byte _getByte(int index);

    // number of bytes in backing byte store
    protected abstract int _getByteLength();

    // start is inclusive, end is exclusive
    protected abstract CharSequence _getSubSequenceForByteBounds(int start, int end);

    @Override
    public int length() {
        // TODO: experiment with getLong() and masking for flag bits; possibly much faster for off-heap impls
        int cacheInstance = packedIndexCache;
        int byteIndex = (int) cacheByteIndex(cacheInstance);
        if (byteIndex == _getByteLength()) {
            // ascii shortcut
           if (cacheInstance >= 0) {
               return _getByteLength();
           } else {
               short charDelta = cacheCharDelta(cacheInstance);
               return charIndex(charDelta, byteIndex);
           }
        }
        short charDelta = cacheCharDelta(cacheInstance);
        for (; byteIndex < _getByteLength(); byteIndex++) {
            byte b = _getByte(byteIndex);
            if (b < 0) {
                // check four-byte first for the over-zealous branch removal strategy
                int continuations = ((int) b & Utf8.FOUR_BYTE_MASK) >> Utf8.FOUR_BYTE_SHIFT;
                charDelta += continuations; // reverse later substraction
                continuations += ((int) b & Utf8.THREE_BYTE_MASK) >> Utf8.THREE_BYTE_SHIFT;
                // always at least two bytes here; we could just use the cont. mask to remove all branches but...
                continuations += 1;
                charDelta -= continuations;
                byteIndex += continuations;
            }
        }
        if (_getByteLength() <= MAX_USHORT) {
            packedIndexCache = packIndexCache(charDelta, _getByteLength());
        }
        return charIndex(charDelta, byteIndex);
    }

    // start is inclusive, end is exclusive
    @Override
    public CharSequence subSequence(int start, int end) {
        int cacheInstance = packedIndexCache;
        int byteIndex = (int) cacheByteIndex(cacheInstance);
        // ascii pre-computed short circuit
        if ((cacheInstance >= 0) && (end <= byteIndex)) {
            return _getSubSequenceForByteBounds(start, end);
        }
        short charDelta = cacheCharDelta(cacheInstance);
        int charIndex = charIndex(charDelta, byteIndex);
        if (charIndex > start) {
            // TODO: reverse scanning from index if guessed to be faster
            charDelta = 0;
            byteIndex = 0;
        }
        // TODO: branch optimization?
        // TODO: 4 byte -> Utf-16 surrogate pairs for java nonsense
        // scan until next character is the requested one
        for (; (charIndex < start) && (byteIndex < _getByteLength()); charIndex++) {
            byte b = _getByte(byteIndex);
            byteIndex += 1;
            // assume well formed and valid index, so all negatives are seq headers
            if (b < 0) {
                // check four-byte first for the over-zealous branch removal strategy
                int continuations = ((int) b & Utf8.FOUR_BYTE_MASK) >> Utf8.FOUR_BYTE_SHIFT;
                charIndex += continuations;
                charDelta += continuations; // reverse later substraction
                continuations += ((int) b & Utf8.THREE_BYTE_MASK) >> Utf8.THREE_BYTE_SHIFT;
                // always at least two bytes here; we could just use the cont. mask to remove all branches but...
                continuations += 1;
                charDelta -= continuations;
                byteIndex += continuations;
            }
        }
        // check to see if we would 'split in half' a surrogate pair -- if java won't stop this madness, we will
        if (charIndex > start) {
            throw new IllegalArgumentException("first character of the requested subsequence is a low-surrogate");
        }
        int startByte = byteIndex;
        for (; (charIndex < end) && (byteIndex < _getByteLength()); charIndex++) {
            byte b = _getByte(byteIndex);
            byteIndex += 1;
            // assume well formed and valid index, so all negatives are seq headers
            if (b < 0) {
                // check four-byte first for the over-zealous branch removal strategy
                int continuations = ((int) b & Utf8.FOUR_BYTE_MASK) >> Utf8.FOUR_BYTE_SHIFT;
                charIndex += continuations;
                charDelta += continuations; // reverse later substraction
                continuations += ((int) b & Utf8.THREE_BYTE_MASK) >> Utf8.THREE_BYTE_SHIFT;
                // always at least two bytes here; we could just use the cont. mask to remove all branches but...
                continuations += 1;
                charDelta -= continuations;
                byteIndex += continuations;
            }
        }
        // check to see if we would 'split in half' a surrogate pair -- if java won't stop this madness, we will
        if (charIndex > end) {
            throw new IllegalArgumentException("last character of the requested subsequence is a high-surrogate");
        }
        int endByte = byteIndex;
        if (byteIndex <= MAX_USHORT) {
            packedIndexCache = packIndexCache(charDelta, byteIndex);
        }
        return _getSubSequenceForByteBounds(startByte, endByte);
    }

    @Override
    public char charAt(int index) {
        int cacheInstance = packedIndexCache;
        int byteIndex = (int) cacheByteIndex(cacheInstance);
        // ascii pre-computed short circuit
        if ((cacheInstance >= 0) && (index < byteIndex)) {
            return (char) _getByte(index);
        }
        short charDelta = cacheCharDelta(cacheInstance);
        int charIndex = charIndex(charDelta, byteIndex);
        if (charIndex > index) {
            // TODO: reverse scanning from index if guessed to be faster
            charDelta = 0;
            byteIndex = 0;
            charIndex = 0;
        }
        // TODO: more branch reduction optimization?
        // TODO: 4 byte -> Utf-16 surrogate pairs for java nonsense
        // scan until next character is the requested one
        for (; (charIndex < index) && (byteIndex < _getByteLength()); charIndex++) {
            byte b = _getByte(byteIndex);
            byteIndex += 1;
            // assume well formed and valid index, so all negatives are seq headers
            if (b < 0) {
                // check four-byte first for the over-zealous branch removal strategy
                int continuations = ((int) b & Utf8.FOUR_BYTE_MASK) >> Utf8.FOUR_BYTE_SHIFT;
                charIndex += continuations;
                charDelta += continuations; // reverse later substraction
                continuations += ((int) b & Utf8.THREE_BYTE_MASK) >> Utf8.THREE_BYTE_SHIFT;
                // always at least two bytes here; we could just use the cont. mask to remove all branches but...
                continuations += 1;
                charDelta -= continuations;
                byteIndex += continuations;
            }
        }
        // check to see if we were asked for a low-surrogate; if so we just passed the four-byter and must rewind
        if (charIndex > index) {
            byteIndex -= 4;
            charDelta += 3;
        }
        // return next char
        byte b = _getByte(byteIndex);
        byteIndex += 1;
        char out;
        if (b < 0) {
            // two-bytes
            if (b < Utf8.MIN_THREE_HEADER) {
                out = (char) ((b & Utf8.TWO_BYTE_HEADER_MASK) << 6);
                byte b2 = _getByte(byteIndex);
                out |= (char) (b2 & Utf8.CONTINUATION_MASK);
                charDelta -= 1;
                byteIndex += 1;
            } else if (b < Utf8.MIN_FOUR_HEADER) { // three-bytes
                out = (char) ((b & Utf8.THREE_BYTE_HEADER_MASK) << (6 + 6));
                byte b2 = _getByte(byteIndex);
                byteIndex += 1;
                out |= (char) ((b2 & Utf8.CONTINUATION_MASK) << 6);
                byte b3 = _getByte(byteIndex);
                byteIndex += 1;
                out |= (char) (b3 & Utf8.CONTINUATION_MASK);
                charDelta -= 2;
            } else { // four-bytes
                int codePoint = (b & Utf8.FOUR_BYTE_HEADER_MASK) << (6 + 6 + 6);
                byte b2 = _getByte(byteIndex);
                byteIndex += 1;
                codePoint |= (b2 & Utf8.CONTINUATION_MASK) << (6 + 6);
                byte b3 = _getByte(byteIndex);
                byteIndex += 1;
                codePoint |= (b3 & Utf8.CONTINUATION_MASK) << 6;
                byte b4 = _getByte(byteIndex);
                byteIndex += 1;
                codePoint |= b4 & Utf8.CONTINUATION_MASK;
                charDelta -= 2;
                // high or low surrogate -- uses same logic from earlier
                if (charIndex > index) {
                    out = Character.lowSurrogate(codePoint);
                } else {
                    out = Character.highSurrogate(codePoint);
                }
            }
        } else {
            out = (char) b;
        }
        if (byteIndex <= MAX_USHORT) {
            packedIndexCache = packIndexCache(charDelta, byteIndex);
        }
        return out;
    }

    /**
     * Should be the same hash as that of a String representing the same
     * sequence of characters. This hash code is _not_ cached (for now).
     * It should be trivial to subclass and implement if really needed.
     */
    @Override
    public int hashCode() {
        // TODO: use better iteration
        int length = length();
        int hash = 0;
        for (int i = 0; i < length; i++) {
            char c = charAt(i);
            hash = 31 * hash + c;
        }
        return hash;
    }

    public String toDebugString() {
        int cacheInstance = packedIndexCache;
        return Objects.toStringHelper(this)
                .add("byteLength", _getByteLength())
                .add("byteIndex", cacheByteIndex(cacheInstance))
                .add("charDelta", cacheCharDelta(cacheInstance))
                .toString();
    }

}
