package com.addthis.basis.chars;

import io.netty.buffer.ByteBuf;

/**
 * See the writable extension "CharBuf" for long-winded high level discussion.
 *
 * This interface subset just omits the modification methods, and may require
 * read-only backing stores. Objects which implement this interface and not
 * CharBuf (since inheritance means they'll always do both etc.) are likely
 * to be thread-safe and read-only, but this interface cannot guarantee that
 * so always check with the implementation.
 *
 * TODO: ConcurrentCharBuf and/ or ImmutableCharBuf markers (interface/ abstract base) desirable?
 */
public interface ReadableCharBuf extends CharSequence, Comparable<ReadableCharBuf> {

    /**
     * Return value should be consistent across CharBuf implementations for the
     * same underlying logical CharSequence.
     */
    @Override
    public int hashCode();

    /**
     * Should return true for any CharBuf that represents the same underlying logical
     * CharSequence.
     */
    @Override
    public boolean equals(Object obj);

    /**
     * Should perform lexicographical comparison.
     */
    @Override
    public int compareTo(ReadableCharBuf o);

    /**
     * Returns a view of this CharSequence as its backing Utf8 bytes as
     * represented by a ByteBuf.
     */
    public ByteBuf toByteBuf();

    // get arbitrary byte from backing byte store
    public abstract byte getByte(int index);

    // number of bytes in backing byte store
    public abstract int getByteLength();

    // start is inclusive, end is exclusive
    public abstract ReadableCharBuf getSubSequenceForByteBounds(int start, int end);
}
