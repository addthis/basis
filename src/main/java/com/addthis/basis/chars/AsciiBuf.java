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

import java.io.IOException;

import java.nio.charset.UnmappableCharacterException;

import com.google.common.annotations.Beta;

import io.netty.buffer.ByteBuf;

@Beta
public class AsciiBuf extends ReadOnlyAsciiBuf implements CharBuf {

    public AsciiBuf(ByteBuf data) {
        super(data);
    }

    public AsciiBuf(CharBuf charBuf) {
        this(charBuf.toByteBuf());
    }

    @Override
    public Appendable append(CharSequence csq) throws IOException {
        return append(csq, 0, csq.length());
    }

    @Override
    public Appendable append(CharSequence csq, int start, int end) throws IOException {
        if (csq.length() < end) {
            throw new IndexOutOfBoundsException();
        }
        if (csq instanceof ReadOnlyAsciiBuf) {
            content().writeBytes(((ReadOnlyAsciiBuf) csq).content());
        } else {
            for (int i = start; i < end; i++) {
                char c =  csq.charAt(i);
                if (c >= '\u0080') {
                    throw new UnmappableCharacterException(2);
                }
                content().writeByte((byte) c);
            }
        }
        return this;
    }

    @Override
    public Appendable append(char c) throws IOException {
        if (c >= '\u0080') {
            throw new UnmappableCharacterException(2);
        }
        content().writeByte((byte) c);
        return this;
    }
}
