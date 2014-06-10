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

import java.nio.charset.StandardCharsets;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.DefaultByteBufHolder;
import io.netty.util.IllegalReferenceCountException;

/**
 * A CharSequence backed by a ByteBuf instead of a String. This
 * version supports all characters. The backing store is a
 * variable width utf8 representation, but for char seq's sake,
 * we may emulate their non-sense semi-variable-width chars to
 * some extent when accessed via the CharSequence interface.
 */
public class ReadOnlyUtfBuf extends AbstractReadOnlyUtfBuf implements ReadableCharBuf, ByteBufHolder {

    private final ByteBuf data;

    public ReadOnlyUtfBuf(ByteBuf data) {
        if (data == null) {
            throw new NullPointerException("data");
        }
        this.data = data;
    }

    // these two are intended to allow for propagating cache metadata to derived buffers
    ReadOnlyUtfBuf(ByteBuf data, int cacheInstance) {
        if (data == null) {
            throw new NullPointerException("data");
        }
        this.data = data;
        this.packedIndexCache = cacheInstance;
    }

    ReadOnlyUtfBuf(ByteBuf data, int charDelta, int byteIndex) {
        this(data, packIndexCache(charDelta, byteIndex));
    }

    public ReadOnlyUtfBuf(ByteBufHolder charBuf) {
        this(charBuf.content());
    }

    @Override
    public byte getByte(int index) {
        return content().getByte(index);
    }

    @Override
    public int getByteLength() {
        return content().readableBytes();
    }

    @Override
    public ReadableCharBuf getSubSequenceForByteBounds(int start, int end) {
        return new ReadOnlyUtfBuf(data.slice(start, end - start));
    }

    @Override
    public String toString() {
        return content().toString(StandardCharsets.UTF_8);
    }

    @Override
    public int compareTo(ReadableCharBuf o) {
        if (o instanceof ReadOnlyUtfBuf) {
            return ByteBufUtil.compare(((ReadOnlyUtfBuf) o).content(), content());
        } else {
            return CharSequenceComparator.INSTANCE.compare(this, o);
        }
    }

    @Override
    public ByteBuf toByteBuf() {
        return content();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ReadableCharBuf) {
            return ByteBufUtil.equals(((ReadableCharBuf) obj).toByteBuf(), content());
        }
        return false;
    }

    @Override
    public ByteBuf content() {
        if (data.refCnt() <= 0) {
            throw new IllegalReferenceCountException(data.refCnt());
        }
        return data;
    }

    @Override
    public ByteBufHolder copy() {
        return new DefaultByteBufHolder(data.copy());
    }

    @Override
    public ByteBufHolder duplicate() {
        return new DefaultByteBufHolder(data.duplicate());
    }

    @Override
    public int refCnt() {
        return data.refCnt();
    }

    @Override
    public ByteBufHolder retain() {
        data.retain();
        return this;
    }

    @Override
    public ByteBufHolder retain(int increment) {
        data.retain(increment);
        return this;
    }

    @Override
    public boolean release() {
        return data.release();
    }

    @Override
    public boolean release(int decrement) {
        return data.release(decrement);
    }
}
