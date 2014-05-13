package com.addthis.basis.chars;

import java.nio.charset.StandardCharsets;

/**
 * Intended for those cases where you just really can't avoid using byte[]s.
 * For instance, interacting with third parties that require, or already provide
 * byte[]s from their interface.
 *
 * Now you _could_ just wrap the byte[] in a ByteBuf, but at high speeds that might
 * be annoyingly expensive. This class will at least let us test that.
 */
public class ByteArrayReadOnlyUtfBuf extends AbstractReadOnlyUtfBuf {

    private final byte[] data;

    public ByteArrayReadOnlyUtfBuf(byte[] data) {
        if (data == null) {
            throw new NullPointerException("data");
        }
        this.data = data;
    }

    // ideally only used for low-performance requirement code; like testing
    public ByteArrayReadOnlyUtfBuf(String javaString) {
        if (javaString == null) {
            throw new NullPointerException("java string");
        }
        this.data = javaString.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected byte _getByte(int index) {
        return data[index];
    }

    @Override
    protected int _getByteLength() {
        return data.length;
    }

    @Override
    protected CharSequence _getSubSequenceForByteBounds(int start, int end) {
        int length = end - start;
        byte[] wastefulClone = new byte[length];
        System.arraycopy(data, start, wastefulClone, 0, length);
        return new ByteArrayReadOnlyUtfBuf(wastefulClone);
    }
}
