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

import io.netty.buffer.ByteBuf;

/**
 * A CharSequence backed by a ByteBuf instead of a String. This
 * version only supports ASCII characters and so while it supports
 * mutation operations, it is best used for immutable Strings that
 * have a known, fixed Charset satisfied by the range of ASCII
 * characters. (Note: it is called Ascii to make it easier to quickly
 * understand, but technically it allows all single-byte UTF-8 chars;
 * including the NULL value 0).
 *
 * This should be particularly helpful for Strings that are
 * quickly or frequently serialized or deserialized and may not
 * even be looked.
 *
 * It uses a backing ByteBuf, so it extends DefaultByteBufHolder,
 * and should be released when no longer being used to ensure the
 * proper release of resources.
 *
 * 7/8 bit efficient note: Since in this case we are restricting the domain
 * of characters to single-bytes, we could theoretically represent them using
 * 1/8th less space. However, for a large number of reasons this would be
 * inconvenient and have other unknown performance properties. Therefore it
 * is left as a future excercise to verify and support that use case.
 *
 * malformed character handling: It might be reasonable to have an option or
 * an implementation, etc, that handles non-ascii characters in a non-
 * exceptional way. The JDK NIO Character encoding jazz has a bit of support
 * for this already. Two most likely use cases: a) transforming accented latin
 * characters into their plain ascii counterparts and b) replacing with '?' or
 * ' ' and the like.
 */
public class AsciiBuf extends ReadOnlyAsciiBuf implements CharBuf {

    public AsciiBuf(ByteBuf data) {
        super(data);
    }

    public AsciiBuf(CharBuf charBuf) {
        this(charBuf.content());
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
