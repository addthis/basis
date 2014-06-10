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

import java.util.Arrays;

import java.nio.charset.StandardCharsets;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

/**
 * Intended for those cases where you just really can't avoid using byte[]s.
 * For instance, interacting with third parties that require, or already provide
 * byte[]s from their interface.
 *
 * Now you _could_ just wrap the byte[] in a ByteBuf, but at high speeds that might
 * be annoyingly expensive. This class will at least let us test that.
 */
public class ByteArrayReadOnlyUtfBuf extends AbstractReadOnlyUtfBuf {

    protected final byte[] data;

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
    public byte getByte(int index) {
        return data[index];
    }

    @Override
    public int getByteLength() {
        return data.length;
    }

    @Override
    public ReadableCharBuf getSubSequenceForByteBounds(int start, int end) {
        int length = end - start;
        byte[] wastefulClone = new byte[length];
        System.arraycopy(data, start, wastefulClone, 0, length);
        return new ByteArrayReadOnlyUtfBuf(wastefulClone);
    }

    @Override
    public int compareTo(ReadableCharBuf o) {
        return CharSequenceComparator.INSTANCE.compare(this, o);
    }

    @Override
    public ByteBuf toByteBuf() {
        return Unpooled.wrappedBuffer(data);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ByteArrayReadOnlyUtfBuf) {
            return Arrays.equals(data, ((ByteArrayReadOnlyUtfBuf) obj).data);
        } else if (obj instanceof ReadableCharBuf) {
            return ByteBufUtil.equals(((ReadableCharBuf) obj).toByteBuf(), toByteBuf());
        }
        return false;
    }
}
