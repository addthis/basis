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

package com.addthis.basis.chars;

import com.google.common.annotations.Beta;
import com.google.common.base.Objects;

/**
 * A CharSequence backed by utf-8 bytes instead of java chars (ie. utf-16 bytes)
 */
@Beta
public abstract class AbstractReadOnlyUtfBuf implements ReadableCharBuf {

    /**
     * thread-safe, lightweight, oppertunistic index cache
     * tracks number of multi-byte chars for a given substring
     * of bytes starting from 0 to a variable end point.
     *
     * it is two numbers packed into one int. The reasoning is
     * that non-volatile int reads/writes are guaranteed to be
     * atomic whereas two shorts (or any length numeral) could
     * sometimes give _wrong_ data instead of just potentially
     * unhelpful data. Longs are not guaranteed to be safe from
     * word tearing so we limit ourselves to working well for up
     * to what an int provides. A subclass/ alt impl. may decide
     * to make other trade-offs re: index cache maximums, thread
     * safety etc. if needed.
     * Their non-volatile nature notably benefits us on two fronts.
     * a) It helps single-threaded use by preventing any memory fencing
     * overhead.
     * b) It helps multi-threaded use by making it more likely for
     * changes to not be visible to each other and thereby making it
     * more likely for the cache to remain helpful in that thread's
     * context. The most likely case being two threads concurrently
     * iterating over it via the CharSequence interface.
     *
     * The first number can only ever be negative. It is the number
     * of 'extra' bytes needed to represent all non-ascii characters.
     * We do not know the exact number of non-ascii characters since
     * each one may use a variable number of bytes. However counting
     * byte-wise allows us to know the number of charactes up to the
     * stored index, and this is the most helpful information. Since
     * in the worst case, this negative number can be up to two times
     * the character index, we pack it into 16 bits + the negative sign
     * bit for 17 total bits. This lets it get up to double the remaining
     * 15 bits for the character index.
     *
     * The second number is the character index that the first one is defined
     * in reference to. If the first number is zero, then this index may
     * be as large as Integer.MAX_VALUE. Otherwise, it is limited to
     * Short.MAX_VALUE. So by taking this number, character index, and
     * subtracting the first number, the byte offset, we end up with the
     * character index + some positive number, which gives us the byte index
     * the next character can be found at.
     *
     * Finally, it is protected (not private) because here there be
     * dragons and it might be helpful. Likely we will provide a
     * 'final' subclass for maximum safe-string-replacing semantics.
     **/
    protected int packedIndexCache;

    protected static final int MAX_USHORT = 65535;
    protected static final int MAX_USHORT_LESS_ONE = 65534;
    protected static final int MAX_USHORT_LESS_FOUR = 65531;

    // upper half of packedIndexCache; must use a locally stored copy of the cache value to be thread safe
    // it is a negative value counting down from zero. this lets us do lazier ascii purity queries/ adds
    protected static int cacheByteOffset(int cacheInstance) {
            return cacheInstance >> 15;
    }

    // lower half of packedIndexCache; must use a locally stored copy of the cache value to be thread safe
    protected static int cacheCharIndex(int cacheInstance) {
        return 0x7FFF & cacheInstance;
    }

    protected static int packIndexCache(int byteOffset, int charIndex) {
        byteOffset <<= 15;
        byteOffset |= charIndex;
        return byteOffset;
    }

    protected static int byteIndex(int byteOffset, int charIndex) {
        return charIndex - byteOffset;
    }

    // returns true if entire string is known to be ascii only
    protected boolean knownAsciiOnly(int cacheInstance) {
        // byte lengths can't be negative so this implicitly fails non-asciis as well
        // as tests for completeness
        return cacheInstance == getByteLength();
    }

    @Override
    public int length() {
        // TODO: experiment with getLong() and masking for flag bits; possibly much faster for off-heap impls
        final int cacheInstance = packedIndexCache;
        if (knownAsciiOnly(cacheInstance)) {
            return cacheInstance;
        }
        int charIndex = cacheCharIndex(cacheInstance);
        int byteOffset = cacheByteOffset(cacheInstance);
        final int byteLength = getByteLength();
        int byteIndex = byteIndex(byteOffset, charIndex);
        if (byteIndex == byteLength) {
            return charIndex;
        }
        for (; byteIndex < byteLength; byteIndex++) {
            byte b = getByte(byteIndex);
            if (b < 0) {
                // check four-byte first for the over-zealous branch removal strategy
                int continuations = ((int) b & Utf8.FOUR_BYTE_MASK) >> Utf8.FOUR_BYTE_SHIFT;
                byteOffset += continuations; // reverse later substraction
                continuations += ((int) b & Utf8.THREE_BYTE_MASK) >> Utf8.THREE_BYTE_SHIFT;
                // always at least two bytes here; we could just use the cont. mask to remove all branches but...
                continuations += 1;
                byteOffset -= continuations;
                byteIndex += continuations;
            }
        }
        // byteOffset is stored as a negative, so remember to add here
        charIndex = byteLength + byteOffset;
        if (charIndex <= Short.MAX_VALUE) {
            packedIndexCache = packIndexCache(byteOffset, charIndex);
        }
        return charIndex;
    }

    private int tryAsciiScan(int start, int end) {
        for (int i = start; i < end; i++) {
            byte b = getByte(i);
            if (b < 0) {
                return i;
            }
        }
        return end;
    }

    private void nonAsciiScan(BufferIndex index, int end) {
        if (index.charIndex > end) {
            // TODO: reverse scanning from index if guessed to be faster
            index.charIndex = 0;
            index.byteOffset = 0;
            index.byteIndex = 0;
        }

        // TODO: branch optimization, bounds check optimizations (maybe?)
        // scan until next character is the requested one
        for (; index.charIndex < end; index.charIndex++) {
            byte b = getByte(index.byteIndex);
            index.byteIndex += 1;
            // assume well formed and valid index, so all negatives are seq headers
            if (b < 0) { // advance past the continuation bytes based on header meta-data
                // check four-byte first for the over-zealous branch removal strategy
                int continuations = ((int) b & Utf8.FOUR_BYTE_MASK) >> Utf8.FOUR_BYTE_SHIFT;
                index.charIndex += continuations;
                index.byteIndex += continuations;

                continuations = ((int) b & Utf8.THREE_BYTE_MASK) >> Utf8.THREE_BYTE_SHIFT;
                // always at least two bytes here; we could just use the cont. mask to remove all branches but...
                continuations += 1;
                index.byteOffset -= continuations;
                index.byteIndex += continuations;
            }
        }
    }

    // start is inclusive, end is exclusive
    // TODO: enforce argument bounds
    @Override
    public ReadableCharBuf subSequence(int start, int end) {
        final int cacheInstance = packedIndexCache;
        // ascii pre-computed short circuit; end must be positive so it enforces ascii only.
        // the comparison checks if the cache knows about all the bytes up to the end index.
        if (end <= cacheInstance) {
            return getSubSequenceForByteBounds(start, end);
        } else if (cacheInstance >= 0) {
            packedIndexCache = tryAsciiScan(cacheInstance, end);
            if (end <= packedIndexCache) {
                return getSubSequenceForByteBounds(start, end);
            }
        }
        BufferIndex index = new BufferIndex(cacheInstance);
        nonAsciiScan(index, start);
        // check to see if we would 'split in half' a surrogate pair -- if java won't stop this madness, we will
        if (index.charIndex > start) {
            throw new IllegalArgumentException("first character of the requested subsequence is a low-surrogate");
        }
        int startByte = index.byteIndex;

        nonAsciiScan(index, end);
        // check to see if we would 'split in half' a surrogate pair -- if java won't stop this madness, we will
        if (index.byteIndex > end) {
            throw new IllegalArgumentException("last character of the requested subsequence is a high-surrogate");
        }
        if (index.charIndex <= Short.MAX_VALUE) {
            packedIndexCache = packIndexCache(index.byteOffset, index.charIndex);
        }
        int endByte = index.byteIndex;
        return getSubSequenceForByteBounds(startByte, endByte);
    }

    private char nextCharForBufferIndex(BufferIndex index, boolean highSurrogate) {
        index.charIndex += 1;
        byte b = getByte(index.byteIndex ++);
        char out;
        if (b >= 0) { // one-byte
            out = (char) b;
        } else if (b < Utf8.MIN_THREE_HEADER) { // two-bytes
            out = (char) ((b & Utf8.TWO_BYTE_HEADER_MASK) << 6);
            b = getByte(index.byteIndex ++);
            out |= (char) (b & Utf8.CONTINUATION_MASK);
            index.byteOffset -= 1;
        } else if (b < Utf8.MIN_FOUR_HEADER) { // three-bytes
            out = (char) ((b & Utf8.THREE_BYTE_HEADER_MASK) << (6 + 6));
            b = getByte(index.byteIndex ++);
            out |= (char) ((b & Utf8.CONTINUATION_MASK) << 6);
            b = getByte(index.byteIndex ++);
            out |= (char) (b & Utf8.CONTINUATION_MASK);
            index.byteOffset -= 2;
        } else { // four-bytes
            int codePoint = (b & Utf8.FOUR_BYTE_HEADER_MASK) << (6 + 6 + 6);
            b = getByte(index.byteIndex ++);
            codePoint |= (b & Utf8.CONTINUATION_MASK) << (6 + 6);
            b = getByte(index.byteIndex ++);
            codePoint |= (b & Utf8.CONTINUATION_MASK) << 6;
            b = getByte(index.byteIndex ++);
            codePoint |= b & Utf8.CONTINUATION_MASK;
            // high or low surrogate
            if (highSurrogate) {
                out = Character.highSurrogate(codePoint);
            } else {
                out = Character.lowSurrogate(codePoint);
            }
            index.charIndex  += 1;
            index.byteOffset -= 2;
        }
        if (index.charIndex <= Short.MAX_VALUE) {
            packedIndexCache = packIndexCache(index.byteOffset, index.charIndex);
        }
        return out;
    }

    @Override
    public char charAt(int index) {
        // ascii short cuts
        byte b = getByte(index); // unused for non-ascii but oh well
        if (index < packedIndexCache) {
            return (char) b;
        } else if (packedIndexCache >= 0) {
            packedIndexCache = tryAsciiScan(packedIndexCache, getByteLength());
            if (index < packedIndexCache) {
                return (char) b;
            }
        }

        BufferIndex bufferIndex = new BufferIndex(packedIndexCache);
        nonAsciiScan(bufferIndex, index);
        // check to see if we were asked for a low-surrogate; if so we just passed the four-byter and must rewind
        boolean highSurrogate = true;
        if (bufferIndex.charIndex > index) {
            bufferIndex.byteIndex -= 4;
            bufferIndex.byteOffset += 2;
            bufferIndex.charIndex -= 2;
            highSurrogate = false;
        }
        return nextCharForBufferIndex(bufferIndex, highSurrogate);
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

    @Override
    public int compareTo(ReadableCharBuf o) {
        return CharSequenceComparator.INSTANCE.compare(this, o);
    }

    public String toDebugString() {
        int cacheInstance = packedIndexCache;
        return Objects.toStringHelper(this)
                .add("byteLength", getByteLength())
                .add("byteIndex", cacheCharIndex(cacheInstance))
                .add("charDelta", cacheByteOffset(cacheInstance))
                .toString();
    }
}
