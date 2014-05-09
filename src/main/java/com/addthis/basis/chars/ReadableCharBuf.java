package com.addthis.basis.chars;

import io.netty.buffer.ByteBufHolder;

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
public interface ReadableCharBuf extends CharSequence, Comparable<ReadableCharBuf>, ByteBufHolder {

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

}
