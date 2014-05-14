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
public class ReadOnlyUtfBuf extends AbstractReadOnlyUtfBuf implements ReadableCharBuf {

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

    ReadOnlyUtfBuf(ByteBuf data, short charDelta, short byteIndex) {
        this(data, packIndexCache(charDelta, byteIndex));
    }

    public ReadOnlyUtfBuf(CharBuf charBuf) {
        this(charBuf.content());
    }

    @Override
    protected byte _getByte(int index) {
        return content().getByte(index);
    }

    @Override
    protected int _getByteLength() {
        return content().readableBytes();
    }

    @Override
    protected CharSequence _getSubSequenceForByteBounds(int start, int end) {
        return new ReadOnlyUtfBuf(data.slice(start, end - start));
    }

    @Override
    public String toString() {
        return content().toString(StandardCharsets.UTF_8);
    }

    @Override
    public int compareTo(ReadableCharBuf o) {
        return ByteBufUtil.compare(o.content(), content());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ReadableCharBuf) {
            return ByteBufUtil.equals(((ReadableCharBuf) obj).content(), content());
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
