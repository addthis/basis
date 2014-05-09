package com.addthis.basis.chars;

import java.nio.charset.UnmappableCharacterException;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.DefaultByteBufHolder;

/**
 * A CharSequence backed by a ByteBuf instead of a String. This
 * version only supports unknown characters. It is read only.
 *
 * This may be helpful when blindly replacing a lot of String
 * usages with CharBufs
 */
public abstract class AbstractReadOnlyCharBuf extends DefaultByteBufHolder implements ReadableCharBuf {

    public AbstractReadOnlyCharBuf(ByteBuf data) {
        this(data, false);
    }

    public AbstractReadOnlyCharBuf(ByteBuf data, boolean validate) {
        super(data);
        if (validate) {
            if (!validate()) {
                throw new RuntimeException(new UnmappableCharacterException(2));
            }
        }
    }

    public AbstractReadOnlyCharBuf(CharBuf charBuf) {
        this(charBuf.content());
    }

    /**
     * Searches the current underlying data for erroneous values. Useful for testing
     * and untrusted constructors.
     * @return true if valid
     */
    protected abstract boolean validate();

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
