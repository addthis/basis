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

@Beta
public class ByteArrayReadOnlyAsciiBuf extends ByteArrayReadOnlyUtfBuf {

    public ByteArrayReadOnlyAsciiBuf(byte[] data) {
        super(data);
    }

    public ByteArrayReadOnlyAsciiBuf(String javaString) {
        super(javaString);
    }

    @Override
    protected boolean knownAsciiOnly(int cacheInstance) {
        return true;
    }

    @Override
    public int length() {
        return getByteLength();
    }

    @Override
    public char charAt(int index) {
        return (char) getByte(index);
    }

    // start is inclusive, end is exclusive
    @Override
    public ReadableCharBuf subSequence(int start, int end) {
        return getSubSequenceForByteBounds(start, end);
    }

    @Override
    public String toString() {
        return new String(data, 0);
    }

    /**
     * Uses the cacheInstance for caching the hash instead
     */
    @Override
    public int hashCode() {
        int hash = packedIndexCache;
        if (hash == 0) {
            int length = length();
            for (int i = 0; i < length; i++) {
                char c = charAt(i);
                hash = 31 * hash + c;
            }
            packedIndexCache = hash;
        }
        return hash;
    }
}
