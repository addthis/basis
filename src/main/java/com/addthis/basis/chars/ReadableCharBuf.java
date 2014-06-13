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
@Beta
public interface ReadableCharBuf extends CharSequence, Comparable<ReadableCharBuf> {

    /** Consistent across implementations for the same underlying character string. */
    @Override
    public int hashCode();

    /** True for any ReadableCharBuf that represents the same underlying character string. */
    @Override
    public boolean equals(Object obj);

    /** Should perform lexicographical comparison. */
    @Override
    public int compareTo(ReadableCharBuf o);

    /** A view of the backing Utf8 bytes as represented by a ByteBuf. */
    public ByteBuf toByteBuf();

    // get arbitrary byte from backing byte store
    public abstract byte getByte(int index);

    // number of bytes in backing byte store
    public abstract int getByteLength();

    // start is inclusive, end is exclusive
    public abstract ReadableCharBuf getSubSequenceForByteBounds(int start, int end);
}
